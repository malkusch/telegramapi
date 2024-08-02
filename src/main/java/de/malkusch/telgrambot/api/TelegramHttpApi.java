package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.reaction.ReactionTypeEmoji;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import de.malkusch.telgrambot.*;
import de.malkusch.telgrambot.Update.CallbackUpdate.CallbackId;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.Collection;

import static de.malkusch.telgrambot.api.PinnedMessageFactory.pinnedMessage;

final class TelegramHttpApi implements TelegramApi {

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

    public void receiveUpdates(UpdateReceiver... receivers) {
        var dispatcher = new UpdateDispatcher(receivers, this);

        var request = new GetUpdates() //
                .timeout((int) timeouts.polling().toSeconds())
                .allowedUpdates("message", "message_reaction", "callback_query");

        monitor.startMonitoring();
        api.setUpdatesListener(monitor.updateListener(dispatcher), monitor.exceptionHandler(dispatcher), request);
    }

    public MessageId send(String message, Button... buttons) {
        var requestButtons = Arrays.stream(buttons) //
                .map(it -> new InlineKeyboardButton(it.name()).callbackData(it.callback().toString())) //
                .toArray(InlineKeyboardButton[]::new);

        var keyboard = new InlineKeyboardMarkup(requestButtons);
        var request = new SendMessage(chatId, message).replyMarkup(keyboard);
        return send(request);
    }

    public void pin(MessageId message) {
        var pin = new PinChatMessage(chatId, message.id()) //
                .disableNotification(true);
        execute(pin);
    }

    public PinnedMessage pinned() {
        var response = execute(new GetChat(chatId));
        return pinnedMessage(response.chat());
    }

    public void unpin(MessageId message) {
        execute(new UnpinChatMessage(chatId).messageId(message.id()));
    }

    public void unpin() {
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

    public void react(MessageId messageId, Reaction reaction) {
        execute(new SetMessageReaction(chatId, messageId.id(), new ReactionTypeEmoji(reaction.emoji())));
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
        var response = api.execute(request);
        if (!response.isOk()) {
            var error = String.format("Sending to Telegram failed: [%d] %s", response.errorCode(), response.description());
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
