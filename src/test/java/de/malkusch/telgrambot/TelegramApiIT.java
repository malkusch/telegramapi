package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.TelegramApi.Button;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDateTime;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TelegramApiIT {

    @RegisterExtension
    static final TelegramApiExtension telegram = new TelegramApiExtension();

    @Test
    public void shouldSendMessage() {
        var message = String.format("shouldSendMessage %s %s", LocalDateTime.now(), randomUUID());

        var id = telegram.api.send(message);

        assertEquals(message, telegram.fetchMessage(id));
        telegram.api.delete(id);
    }

    @Test
    public void shouldSendMessageWithButtons() {
        var message = String.format("shouldSendMessage %s %s", LocalDateTime.now(), randomUUID());
        var button1 = new Button("button1", new Callback(new Command("command1"), "payload"));
        var button2 = new Button("button2", new Command("command2"));

        var id = telegram.api.send(message, button1, button2);

        assertEquals(message, telegram.fetchMessage(id));
        telegram.api.delete(id);
    }

}
