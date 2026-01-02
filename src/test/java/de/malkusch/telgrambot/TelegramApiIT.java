package de.malkusch.telgrambot;

import de.malkusch.telgrambot.PinnedMessage.CallbackMessage;
import de.malkusch.telgrambot.PinnedMessage.TextMessage;
import de.malkusch.telgrambot.TelegramApi.Button;
import de.malkusch.telgrambot.api.TelegramTestApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDateTime;

import static de.malkusch.telgrambot.PinnedMessage.NO_MESSAGE;
import static de.malkusch.telgrambot.Reaction.THUMBS_UP;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TelegramApiIT {

    @RegisterExtension
    static final TelegramTestApi api = new TelegramTestApi();

    @Test
    public void shouldSendMessage() {
        var message = String.format("shouldSendMessage %s %s", LocalDateTime.now(), randomUUID());

        var id = api.send(message);

        assertEquals(new TextMessage(id, message), api.fetchMessage(id));
    }

    @Test
    public void shouldSendMessageWithButtons() {
        var message = String.format("shouldSendMessageWithButtons %s %s", LocalDateTime.now(), randomUUID());
        var button1 = new Button("button1", new Callback(new Command("command1"), "payload"));
        var button2 = new Button("button2", new Command("command2"));
        var button3 = new Button("button3", new Callback(new Command("12345"),
                "6789 123456789 123456789 123456789 123456789 123456789 123"));

        var id = api.send(message, button1, button2, button3);

        assertEquals(new CallbackMessage(id, message, button1, button2, button3), api.fetchMessage(id));
    }

    @Test
    public void shouldDisableButtons() {
        var message = String.format("shouldDisableButtons %s %s", LocalDateTime.now(), randomUUID());
        var button1 = new Button("button1", new Callback(new Command("command1"), "payload"));
        var button2 = new Button("button2", new Command("command2"));
        var id = api.send(message, button1, button2);

        api.disableButtons(id);

        assertEquals(new TextMessage(id, message), api.fetchMessage(id));
    }

    @Test
    public void shouldReact() {
        var message = String.format("shouldReact %s %s", LocalDateTime.now(), randomUUID());
        var id = api.send(message);

        api.react(id, THUMBS_UP);
    }

    @Test
    public void shouldPinMessage() {
        var message = String.format("shouldPinMessage %s %s", LocalDateTime.now(), randomUUID());
        var id = api.send(message);

        api.pin(id);

        var pinned = api.pinned();
        assertEquals(new TextMessage(id, message), pinned);
    }

    @Test
    public void shouldUnpinMessage() {
        var message = String.format("shouldUnpinMessage %s %s", LocalDateTime.now(), randomUUID());
        var id = api.send(message);
        api.pin(id);

        api.unpin(id);

        var pinned = api.pinned();
        assertEquals(NO_MESSAGE, pinned);
    }

    @Test
    public void shouldUnpinAllMessages() {
        var message = String.format("shouldUnpinAllMessages %s %s", LocalDateTime.now(), randomUUID());
        api.pin(api.send(message));
        api.pin(api.send(message));
        api.pin(api.send(message));

        api.unpin();

        var pinned = api.pinned();
        assertEquals(NO_MESSAGE, pinned);
    }
}
