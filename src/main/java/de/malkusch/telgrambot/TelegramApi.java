package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.Message.CallbackMessage.CallbackId;
import de.malkusch.telgrambot.api.TelegramApiFactory;

import java.time.Duration;
import java.util.Collection;

import static java.util.Arrays.asList;

public interface TelegramApi extends AutoCloseable {

    static TelegramApi telegramApi(String chatId, String token, Duration timeout) {
        return TelegramApiFactory.telegramApi(chatId, token, timeout);
    }

    default void startDispatcher(Handler... handlers) {
        startDispatcher(asList(handlers));
    }

    void startDispatcher(Collection<Handler> handlers);

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

    record Reaction(String emoji) {
        public static final Reaction THUMBS_UP = new Reaction("👍");
    }

    void react(MessageId message, Reaction reaction);

    void answer(CallbackId id);

    void answer(CallbackId id, String alert);

    MessageId send(String message);

    void dropPendingUpdates();

}
