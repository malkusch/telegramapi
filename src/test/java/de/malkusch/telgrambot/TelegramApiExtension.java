package de.malkusch.telgrambot;

import com.pengrad.telegrambot.request.GetChat;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfPR
public class TelegramApiExtension implements BeforeEachCallback, AfterEachCallback {
    private final String chatId = System.getenv("TELEGRAM_CHAT_ID");
    private final String token = System.getenv("TELEGRAM_TOKEN");
    public TelegramApi api;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        assertTrue(token != null && chatId != null);
        assertTrue(!token.isBlank() && !chatId.isBlank());

        api = new TelegramApi(chatId, token, Duration.ofSeconds(3));
        api.unpin();
        api.dropPendingUpdates();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        api.unpin();
        api.dropPendingUpdates();
        api.close();
    }

    public String fetchMessage(MessageId messageId) {
        api.pin(messageId);
        try {
            var request = new GetChat(chatId);
            var response = api.execute(request);
            if (response.chat().pinnedMessage() == null) {
                return null;
            }
            var lastMessage = response.chat().pinnedMessage().text();
            return lastMessage;

        } finally {
            api.unpin(messageId);
        }
    }
}
