package de.malkusch.telgrambot;

import com.pengrad.telegrambot.request.GetChat;
import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.TelegramApi.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import static de.malkusch.telgrambot.Message.ReactionMessage.Reaction.THUMBS_UP;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfPR
public class TelegramApiIT {

    private final String chatId = System.getenv("TELEGRAM_CHAT_ID");
    private final String token = System.getenv("TELEGRAM_TOKEN");
    private TelegramApi api;

    @BeforeEach
    public void setup() {
        assertTrue(token != null && chatId != null);
        assertTrue(!token.isBlank() && !chatId.isBlank());

        api = new TelegramApi(chatId, token, Duration.ofSeconds(3));
        api.unpin();
        api.dropPendingUpdates();
    }

    @AfterEach
    public void teardown() throws Exception {
        api.unpin();
        api.dropPendingUpdates();
        api.close();
    }

    @Test
    public void shouldSendMessage() {
        var message = String.format("shouldSendMessage %s %s", LocalDateTime.now(), randomUUID());

        var id = api.send(message);

        assertEquals(message, fetchMessage(id));
        api.delete(id);
    }

    @Test
    public void shouldSendMessageWithButtons() {
        var message = String.format("shouldSendMessage %s %s", LocalDateTime.now(), randomUUID());
        var button1 = new Button("button1", new Callback(new Command("command1"), "payload"));
        var button2 = new Button("button2", new Command("command2"));

        var id = api.send(message, button1, button2);

        assertEquals(message, fetchMessage(id));
        api.delete(id);
    }

    private String fetchMessage(MessageId messageId) {
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
