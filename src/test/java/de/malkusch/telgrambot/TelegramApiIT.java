package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Update.CallbackUpdate.Callback;
import de.malkusch.telgrambot.PinnedMessage.CallbackMessage;
import de.malkusch.telgrambot.PinnedMessage.TextMessage;
import de.malkusch.telgrambot.TelegramApi.Button;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDateTime;

import static de.malkusch.telgrambot.PinnedMessage.NO_MESSAGE;
import static de.malkusch.telgrambot.TelegramApi.Reaction.THUMBS_UP;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TelegramApiIT {

    @RegisterExtension
    static final TelegramBotTestExtension telegram = new TelegramBotTestExtension();

    @Test
    public void shouldSendMessage() {
        var message = String.format("shouldSendMessage %s %s", LocalDateTime.now(), randomUUID());

        var id = telegram.send(message);

        assertEquals(new TextMessage(id, message), telegram.fetchMessage(id));
    }

    @Test
    public void shouldSendMessageWithButtons() {
        var message = String.format("shouldSendMessageWithButtons %s %s", LocalDateTime.now(), randomUUID());
        var button1 = new Button("button1", new Callback(new Command("command1"), "payload"));
        var button2 = new Button("button2", new Command("command2"));

        var id = telegram.send(message, button1, button2);

        assertEquals(new CallbackMessage(id, message, button1, button2), telegram.fetchMessage(id));
    }

    @Test
    public void shouldDisableButtons() {
        var message = String.format("shouldDisableButtons %s %s", LocalDateTime.now(), randomUUID());
        var button1 = new Button("button1", new Callback(new Command("command1"), "payload"));
        var button2 = new Button("button2", new Command("command2"));
        var id = telegram.send(message, button1, button2);

        telegram.disableButton(id);

        assertEquals(new TextMessage(id, message), telegram.fetchMessage(id));
    }

    @Test
    public void shouldReact() {
        var message = String.format("shouldReact %s %s", LocalDateTime.now(), randomUUID());
        var id = telegram.send(message);

        telegram.react(id, THUMBS_UP);
    }

    @Test
    public void shouldPinMessage() {
        var message = String.format("shouldPinMessage %s %s", LocalDateTime.now(), randomUUID());
        var id = telegram.send(message);

        telegram.pin(id);

        var pinned = telegram.pinned();
        assertEquals(new TextMessage(id, message), pinned);
    }

    @Test
    public void shouldUnpinMessage() {
        var message = String.format("shouldUnpinMessage %s %s", LocalDateTime.now(), randomUUID());
        var id = telegram.send(message);
        telegram.pin(id);

        telegram.unpin(id);

        var pinned = telegram.pinned();
        assertEquals(NO_MESSAGE, pinned);
    }

    @Test
    public void shouldUnpinAllMessages() {
        var message = String.format("shouldUnpinAllMessages %s %s", LocalDateTime.now(), randomUUID());
        telegram.pin(telegram.send(message));
        telegram.pin(telegram.send(message));
        telegram.pin(telegram.send(message));

        telegram.unpin();

        var pinned = telegram.pinned();
        assertEquals(NO_MESSAGE, pinned);
    }
}
