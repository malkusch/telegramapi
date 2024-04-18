package de.malkusch.telgrambot;

import java.util.List;

import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.Message.ReactionMessage.Reaction;

record MessageId(int id) {

}

public sealed interface Message {

    record TextMessage(MessageId id, String message, boolean fromBot) implements Message {
    }

    record ReactionMessage(MessageId id, List<Reaction> reactions, boolean fromBot) implements Message {

        public boolean contains(Reaction reaction) {
            return reactions.contains(reaction);
        }

        public static enum Reaction {
            THUMBS_UP, IGNORED
        }
    }

    record CallbackMessage(MessageId id, CallbackId callbackId, Callback callback) implements Message {

        public record CallbackId(String id) {
        }
        
        public record Callback(Command command, String data) {

            public Callback(Command command) {
                this(command, "null");
            }
            
            public static Callback parse(String callback) {
                var parsed = callback.split(":", 2);
                var command = new Command(parsed[0]);
                var data = parsed[1];
                return new Callback(command, data);
            }

            @Override
            public String toString() {
                return command.name() + ":" + data;
            }
        }
    }

    record UnknownMessage() implements Message {
    }
}
