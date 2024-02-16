package de.malkusch.telgrambot;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.PinChatMessage;
import com.pengrad.telegrambot.request.UnpinChatMessage;

@DisabledIfPR
public class TelegramApiIT {

    private final String chatId = System.getenv("TELEGRAM_CHAT_ID");
    private final String token = System.getenv("TELEGRAM_TOKEN");
    private TelegramApi api;

    @BeforeEach
    public void setup() {
        assertTrue(token != null && chatId != null);
        assertTrue(!token.isBlank() && !chatId.isBlank());

        var timeout = Duration.ofSeconds(10);
        api = new TelegramApi(chatId, token, timeout);
    }

    @Test
    public void shouldSendMessage() {
        var message = String.format("Test %s %s", LocalDateTime.now(), randomUUID());

        var id = api.send(message);

        assertEquals(message, fetchMessage(id));
        delete(id);
    }

    private String fetchMessage(MessageId messageId) {
        api.execute(new PinChatMessage(chatId, messageId.id()));
        try {
            var request = new GetChat(chatId);
            var response = api.execute(request);
            if (response.chat().pinnedMessage() == null) {
                return null;
            }
            var lastMessage = response.chat().pinnedMessage().text();
            return lastMessage;

        } finally {
            api.execute(new UnpinChatMessage(chatId).messageId(messageId.id()));
        }
    }

    private void delete(MessageId messageId) {
        var request = new DeleteMessage(chatId, messageId.id());
        api.execute(request);
    }
}
