package de.malkusch.telgrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.reaction.ReactionTypeEmoji;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.Message.CallbackMessage.CallbackId;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import static de.malkusch.telgrambot.PinnedMessage.pinnedMessage;
import static java.lang.Math.round;
import static java.util.Arrays.asList;

public final class TelegramApi implements AutoCloseable {

    final TelegramBot api;
    private final String chatId;
    final Timeouts timeouts;
    private final ConnectionMonitoring monitor;

    private final RateLimiter groupLimit;
    private final RateLimiter messageLimit;
    private final RateLimiter pinLimit;

    public record Timeouts(Duration io, Duration polling) {

        public Timeouts(Duration io) {
            this(io, multiply(io, 10));
        }

        public Duration monitoring() {
            return polling(2);
        }

        public Duration call() {
            return polling(1.2);
        }

        public Duration groupThrottle() {
            return polling(1.1);
        }

        public Duration messageThrottle() {
            return polling(1.1);
        }

        public Duration pinThrottle() {
            return polling(0.5);
        }

        public Duration updateSleep() {
            return io();
        }

        public Duration ping() {
            return polling(0.8);
        }

        private Duration polling(double factor) {
            return multiply(polling, factor);
        }

        private Duration io(double factor) {
            return multiply(io, factor);
        }

        private static Duration multiply(Duration duration, double factor) {
            return Duration.ofMillis(round(duration.toMillis() * factor));
        }
    }

    public TelegramApi(String chatId, String token, Duration timeout) {
        this(chatId, token, new Timeouts(timeout));
    }

    public TelegramApi(String chatId, String token, Timeouts timeouts) {
        this.chatId = chatId;
        this.timeouts = timeouts;
        monitor = new ConnectionMonitoring(this);
        groupLimit = new RateLimiter(Duration.ofMinutes(1), 19, timeouts.groupThrottle(), "group");
        messageLimit = new RateLimiter(Duration.ofSeconds(1), 29, timeouts.messageThrottle(), "message");
        pinLimit = new RateLimiter(Duration.ofSeconds(1), 2, timeouts.pinThrottle(), "pin");
        this.api = buildApi(token);

    }

    private TelegramBot buildApi(String token) {

        var http = new OkHttpClient.Builder() //
                .callTimeout(timeouts.call()) //
                .pingInterval(timeouts.ping()) //
                .connectTimeout(timeouts.io()) //
                .writeTimeout(timeouts.io()) //
                .readTimeout(timeouts.io()) //
                .retryOnConnectionFailure(true) //
                .addInterceptor(monitor.interceptor()) //
                .build();

        return new TelegramBot.Builder(token) //
                .okHttpClient(http) //
                .updateListenerSleep(timeouts.updateSleep().toMillis()) //
                .build();
    }

    public void startDispatcher(Handler... handlers) {
        startDispatcher(asList(handlers));
    }

    public void startDispatcher(Collection<Handler> handlers) {
        var dispatcher = new MessageDispatcher(handlers, this);

        var request = new GetUpdates() //
                .timeout((int) timeouts.polling.toSeconds())
                .allowedUpdates("message", "message_reaction", "callback_query");

        monitor.startMonitoring();
        api.setUpdatesListener(monitor.updateListener(dispatcher), monitor.exceptionHandler(dispatcher), request);
    }

    public record Button(String name, Callback callback) {

        public Button(String name, Command command) {
            this(name, new Callback(command));
        }
    }

    public MessageId send(String message, Button... buttons) {
        var requestButtons = Arrays.stream(buttons) //
                .map(it -> new InlineKeyboardButton(it.name).callbackData(it.callback.toString())) //
                .toArray(InlineKeyboardButton[]::new);

        var keyboard = new InlineKeyboardMarkup(requestButtons);
        var request = new SendMessage(chatId, message).replyMarkup(keyboard);
        return send(request);
    }

    public void pin(MessageId message) {
        pinLimit.acquire();
        var pin = new PinChatMessage(chatId, message.id()) //
                .disableNotification(true);
        execute(pin);
    }

    public PinnedMessage pinned() {
        pinLimit.acquire();
        var response = api.execute(new GetChat(chatId));
        return pinnedMessage(response.chat());
    }

    public void unpin(MessageId message) {
        pinLimit.acquire();
        execute(new UnpinChatMessage(chatId).messageId(message.id()));
    }

    public void unpin() {
        pinLimit.acquire();
        execute(new UnpinAllChatMessages(chatId));
    }

    public void delete(MessageId message) {
        execute(new DeleteMessage(chatId, message.id()));
    }

    public void delete(Collection<MessageId> messages) {
        var intMessages = messages.stream().mapToInt(MessageId::id).toArray();
        execute(new DeleteMessages(chatId, intMessages));
    }

    public void disableButton(MessageId messageId) {
        try {
            execute(new EditMessageReplyMarkup(chatId, messageId.id()));

        } catch (Exception e) {
            // Ignore
        }
    }

    public static record Reaction(String emoji) {
        public static final Reaction THUMBS_UP = new Reaction("👍");
    }

    public void react(MessageId messageId, Reaction reaction) {
        execute(new SetMessageReaction(chatId, messageId.id(), new ReactionTypeEmoji(reaction.emoji)));
    }

    public void answer(CallbackId id) {
        execute(new AnswerCallbackQuery(id.id()));
    }

    public void answer(CallbackId id, String alert) {
        execute(new AnswerCallbackQuery(id.id()).text(alert).showAlert(true));
    }

    public MessageId send(String message) {
        return send(new SendMessage(chatId, message));
    }

    private MessageId send(SendMessage request) {
        var response = execute(request);
        if (response.message() == null) {
            throw new RuntimeException("Sending to Telegram failed: empty message");
        }
        return new MessageId(response.message().messageId());
    }

    public void dropPendingUpdates() {
        var request = new DeleteWebhook() //
                .dropPendingUpdates(true);
        execute(request);
    }

    <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
        messageLimit.acquire();
        groupLimit.acquire();
        var response = api.execute(request);
        if (!response.isOk()) {
            var error = String.format("Sending to Telegram failed: [%d] %s", response.errorCode(), response.description());
            throw new RuntimeException(error);
        }
        return response;
    }

    @Override
    public void close() throws Exception {
        try (monitor; groupLimit; messageLimit; pinLimit) {

        } finally {
            try {
                api.removeGetUpdatesListener();

            } finally {
                api.shutdown();
            }
        }
    }
}
