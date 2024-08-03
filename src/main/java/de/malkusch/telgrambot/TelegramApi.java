package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Update.CallbackUpdate.CallbackId;
import de.malkusch.telgrambot.api.TelegramApiFactory;
import de.malkusch.telgrambot.api.Timeouts;

import java.time.Duration;
import java.util.Collection;

public interface TelegramApi extends AutoCloseable {

    static TelegramApi telegramApi(String chatId, String token, Timeouts timeouts) {
        return TelegramApiFactory.telegramApi(chatId, token, timeouts);
    }

    static TelegramApi telegramApi(String chatId, String token, Duration timeout) {
        return telegramApi(chatId, token, new Timeouts(timeout));
    }

    void receiveUpdates(UpdateReceiver... receivers);

    record Button(String name, Callback callback) {
        public Button(String name, Command command) {
            this(name, new Callback(command));
        }
    }

    MessageId send(String message, Button... buttons);

    void pin(MessageId message);

    PinnedMessage pinned();

    void unpin(MessageId message);

    void unpin();

    void delete(MessageId message);

    void delete(Collection<MessageId> messages);

    void disableButton(MessageId message);

    void react(MessageId message, Reaction reaction);

    void answer(CallbackId id);

    void answer(CallbackId id, String alert);

    MessageId send(String message);

    void dropPendingUpdates();

}
