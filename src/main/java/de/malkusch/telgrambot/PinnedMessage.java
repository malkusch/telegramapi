package de.malkusch.telgrambot;

import com.pengrad.telegrambot.model.ChatFullInfo;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public sealed interface PinnedMessage {

    NoMessage NO_MESSAGE = new NoMessage();
    record NoMessage() implements PinnedMessage {
    }

    record TextMessage(MessageId id, String text) implements PinnedMessage {
    }

    record CallbackMessage(MessageId id, String text, List<Button> buttons) implements PinnedMessage {

        CallbackMessage(MessageId id, String text, Button... buttons) {
            this(id, text, asList(buttons));
        }

        CallbackMessage(MessageId id, String text, TelegramApi.Button... buttons) {
            this(id, text, stream(buttons).map(Button::new).toList());
        }

        public record Button(String name, String callback) {

            Button(TelegramApi.Button button) {
                this(button.name(), button.callback().toString());
            }

        }
    }
}
