package de.malkusch.telgrambot;

import java.util.List;

public sealed interface Update {

    record TextMessage(MessageId id, String message, boolean fromBot) implements Update {
    }

    record ReactionUpdate(MessageId id, List<Reaction> reactions, boolean fromBot) implements Update {

        public boolean contains(Reaction reaction) {
            return reactions.contains(reaction);
        }
    }

    record CallbackUpdate(MessageId id, CallbackId callbackId, Callback callback) implements Update {

        public record CallbackId(String id) {
        }
    }

    record UnknownUpdate() implements Update {
    }
}