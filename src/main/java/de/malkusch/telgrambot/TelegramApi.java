package de.malkusch.telgrambot;

import static java.lang.Math.round;

import java.time.Duration;
import java.util.Collection;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;

import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.Message.CallbackMessage.CallbackId;
import okhttp3.OkHttpClient;

public final class TelegramApi implements AutoCloseable {

    final TelegramBot api;
    private final String chatId;
    final Timeouts timeouts;
    private final ConnectionMonitoring monitor;

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

        public Duration updateSleep() {
            return io();
        }

        public Duration ping() {
            return polling(0.8);
        }

        private Duration polling(double factor) {
            return multiply(polling, factor);
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

    public void startDispatcher(Collection<Handler> handlers) {
        var dispatcher = new MessageDispatcher(handlers, this);

        var request = new GetUpdates() //
                .timeout((int) timeouts.polling.toSeconds())
                .allowedUpdates("message", "message_reaction", "callback_query");
        api.setUpdatesListener(monitor.updateListener(dispatcher), monitor.exceptionHandler(dispatcher), request);
    }

    public record Button(String name, Callback callback) {
    }

    public MessageId send(String message, Button button) {
        var requestButton = new InlineKeyboardButton(button.name).callbackData(button.callback.toString());
        var keyboard = new InlineKeyboardMarkup(requestButton);
        var request = new SendMessage(chatId, message).replyMarkup(keyboard);
        return send(request);
    }

    public void disableButton(MessageId messageId) {
        try {
            execute(new EditMessageReplyMarkup(chatId, messageId.id()));

        } catch (Exception e) {
            // Ignore
        }
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

    <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
        var response = api.execute(request);
        if (!response.isOk()) {
            var error = String.format("Sending to Telegram failed: %d", response.errorCode());
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
