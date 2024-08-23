package de.malkusch.telgrambot;

import java.util.List;

import static java.util.Objects.requireNonNull;

public sealed interface Update {

    record TextMessage(MessageId id, String message, boolean fromBot) implements Update {

        public TextMessage {
            requireNonNull(id);
            requireNonNull(message);
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be empty");
            }
        }
    }

    record ReactionUpdate(MessageId id, List<Reaction> reactions, boolean fromBot) implements Update {

        public ReactionUpdate {
            requireNonNull(id);
            requireNonNull(reactions);
            if (reactions.isEmpty()) {
                throw new IllegalArgumentException("reactions must not be empty");
            }
        }

        public boolean contains(Reaction reaction) {
            return reactions.contains(reaction);
        }
    }

    record CallbackUpdate(MessageId id, CallbackId callbackId, Callback callback) implements Update {

        public CallbackUpdate {
            requireNonNull(id);
            requireNonNull(callbackId);
            requireNonNull(callback);
        }

        public record CallbackId(String id) {
        }
    }

    record UnknownUpdate() implements Update {
    }
}