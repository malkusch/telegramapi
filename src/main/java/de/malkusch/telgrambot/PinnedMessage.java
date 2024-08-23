package de.malkusch.telgrambot;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public sealed interface PinnedMessage {

    NoMessage NO_MESSAGE = new NoMessage();

    record NoMessage() implements PinnedMessage {
    }

    record TextMessage(MessageId id, String text) implements PinnedMessage {

        public TextMessage {
            requireNonNull(id);
            requireNonNull(text);
            if (text.isBlank()) {
                throw new IllegalArgumentException("text must not be empty");
            }
        }
    }

    record CallbackMessage(MessageId id, String text, List<Button> buttons) implements PinnedMessage {

        public CallbackMessage {
            requireNonNull(id);

            requireNonNull(text);
            if (text.isBlank()) {
                throw new IllegalArgumentException("text must not be empty");
            }

            requireNonNull(buttons);
            if (buttons.isEmpty()) {
                throw new IllegalArgumentException("buttons must not be empty");
            }
        }

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
