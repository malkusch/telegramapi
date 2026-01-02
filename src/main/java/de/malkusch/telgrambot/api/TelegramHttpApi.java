package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.reaction.ReactionTypeEmoji;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import de.malkusch.telgrambot.MessageId;
import de.malkusch.telgrambot.PinnedMessage;
import de.malkusch.telgrambot.Reaction;
import de.malkusch.telgrambot.Update.CallbackUpdate.CallbackId;
import de.malkusch.telgrambot.UpdateReceiver;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.Collection;

import static de.malkusch.telgrambot.api.PinnedMessageFactory.pinnedMessage;
import static java.util.Objects.requireNonNull;

final class TelegramHttpApi implements InternalTelegramApi {

    private final TelegramBot api;
    private final String chatId;
    private final Timeouts timeouts;
    private final ConnectionMonitoring monitor;

    public TelegramHttpApi(String chatId, String token, Timeouts timeouts) {
        this.chatId = chatId;
        this.timeouts = timeouts;
        this.monitor = new ConnectionMonitoring(timeouts);
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

    @Override
    public void receiveUpdates(Decorator<UpdatesListener> listenerDecorator, Decorator<ExceptionHandler> errorDecorator, UpdateReceiver... receivers) {
        requireNonNull(receivers);
        if (receivers.length == 0) {
            throw new IllegalArgumentException("Receivers must not be empty");
        }

        var dispatcher = new UpdateDispatcher(receivers, this);

        var request = new GetUpdates() //
                .timeout((int) timeouts.polling().toSeconds())
                .allowedUpdates("message", "message_reaction", "callback_query");

        monitor.startMonitoring();
        api.setUpdatesListener(
                listenerDecorator.decorate(monitor.updateListener(dispatcher)),
                errorDecorator.decorate(monitor.exceptionHandler(dispatcher)),
                request);
    }

    public void dropPendingUpdates() {
        var request = new DeleteWebhook() //
                .dropPendingUpdates(true);
        execute(request);
    }

    public MessageId send(String message, Button... buttons) {
        requireNonNull(message);
        requireNonNull(buttons);
        if (buttons.length == 0) {
            throw new IllegalArgumentException("buttons must not be empty");
        }

        var requestButtons = Arrays.stream(buttons) //
                .map(it -> new InlineKeyboardButton(it.name()).callbackData(it.callback().toString())) //
                .toArray(InlineKeyboardButton[]::new);

        var keyboard = new InlineKeyboardMarkup(requestButtons);
        var request = new SendMessage(chatId, message).replyMarkup(keyboard);
        return send(request);
    }

    public MessageId send(String message) {
        requireNonNull(message);
        return send(new SendMessage(chatId, message));
    }

    private MessageId send(SendMessage request) {
        var response = execute(request);
        if (response.message() == null) {
            throw new RuntimeException("Sending to Telegram failed: empty message");
        }
        return new MessageId(response.message().messageId());
    }

    public void pin(MessageId message) {
        requireNonNull(message);
        var pin = new PinChatMessage(chatId, message.id()) //
                .disableNotification(true);
        execute(pin);
    }

    public PinnedMessage pinned() {
        var response = execute(new GetChat(chatId));
        return pinnedMessage(response.chat());
    }

    public void unpin(MessageId message) {
        requireNonNull(message);
        execute(new UnpinChatMessage(chatId).messageId(message.id()));
    }

    public void unpin() {
        execute(new UnpinAllChatMessages(chatId));
    }

    public void delete(MessageId message) {
        requireNonNull(message);
        execute(new DeleteMessage(chatId, message.id()));
    }

    public void delete(Collection<MessageId> messages) {
        requireNonNull(messages);
        if (messages.isEmpty()) {
            return;
        }

        var intMessages = messages.stream().mapToInt(MessageId::id).toArray();
        execute(new DeleteMessages(chatId, intMessages));
    }

    public void disableButtons(MessageId message) {
        requireNonNull(message);
        try {
            execute(new EditMessageReplyMarkup(chatId, message.id()));
        } catch (Exception e) {
            // Ignore
        }
    }

    public void react(MessageId message, Reaction reaction) {
        requireNonNull(message);
        requireNonNull(reaction);
        execute(new SetMessageReaction(chatId, message.id(), new ReactionTypeEmoji(reaction.emoji())));
    }

    public void answer(CallbackId id) {
        requireNonNull(id);
        execute(new AnswerCallbackQuery(id.id()));
    }

    public void answer(CallbackId id, String alert) {
        requireNonNull(id);
        requireNonNull(alert);
        execute(new AnswerCallbackQuery(id.id()).text(alert).showAlert(true));
    }

    <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
        var response = api.execute(request);
        if (!response.isOk()) {
            var error = String.format("Sending to Telegram failed: [%d] %s\n%s", response.errorCode(), response.description(), request.toWebhookResponse());
            throw new RuntimeException(error);
        }
        return response;
    }

    @Override
    public void close() throws Exception {
        try (monitor) {

        } finally {
            try {
                api.removeGetUpdatesListener();

            } finally {
                api.shutdown();
            }
        }
    }
}
