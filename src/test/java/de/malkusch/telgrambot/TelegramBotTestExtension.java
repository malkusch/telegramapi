package de.malkusch.telgrambot;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfPR
public class TelegramBotTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private final String chatId = System.getenv("TELEGRAM_CHAT_ID");
    private final String token = System.getenv("TELEGRAM_TOKEN");
    public TelegramApi api;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        assertTrue(token != null && chatId != null);
        assertTrue(!token.isBlank() && !chatId.isBlank());

        api = new TelegramApi(chatId, token, Duration.ofSeconds(10));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        api.unpin();
        api.dropPendingUpdates();
    }

    private final List<MessageId> messages = new CopyOnWriteArrayList<>();

    public MessageId send(Function<TelegramApi, MessageId> send) {
        var message = send.apply(api);
        messages.add(message);
        return message;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try (var closing = api) {
            api.unpin();
            api.delete(messages);
            messages.clear();
            api.dropPendingUpdates();
        }
    }

    public String fetchMessage(MessageId messageId) {
        api.pin(messageId);
        try {
            return api.pinned().map(Message.TextMessage::message).orElse(null);

        } finally {
            api.unpin(messageId);
        }
    }

}
