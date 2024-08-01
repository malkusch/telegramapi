package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Message.CallbackMessage;
import de.malkusch.telgrambot.Message.ReactionMessage;
import de.malkusch.telgrambot.Message.ReactionMessage.Reaction;
import de.malkusch.telgrambot.Message.TextMessage;

import java.util.Optional;

public sealed interface Handler {

    void handle(TelegramApi api, Message message);

    record TextHandler(Command command, Handling handler) implements Handler {

        @FunctionalInterface
        public interface Handling {
            void handle(TelegramApi api);
        }

        @Override
        public void handle(TelegramApi api, Message message) {
            if (!(message instanceof TextMessage text)) {
                return;
            }
            if (text.fromBot()) {
                return;
            }
            if (!text.message().equalsIgnoreCase(command.name())) {
                return;
            }
            handler.handle(api);
        }
    }

    record ReactionHandler(Reaction reaction, Handling handler) implements Handler {

        @FunctionalInterface
        public interface Handling {
            void handle(TelegramApi api, ReactionMessage message);
        }

        @Override
        public void handle(TelegramApi api, Message message) {
            if (!(message instanceof ReactionMessage reactionMessage)) {
                return;
            }
            if (reactionMessage.fromBot()) {
                return;
            }
            if (!reactionMessage.contains(reaction)) {
                return;
            }
            handler.handle(api, reactionMessage);
        }
    }

    record CallbackHandler(Command command, Handling handler) implements Handler {

        public record Result(boolean disableButton, Optional<String> alert,
                             Optional<de.malkusch.telgrambot.TelegramApi.Reaction> reaction) {

            public Result(boolean disableButton) {
                this(disableButton, Optional.empty(), Optional.empty());
            }

            public Result(boolean disableButton, de.malkusch.telgrambot.TelegramApi.Reaction reaction) {
                this(disableButton, Optional.empty(), Optional.of(reaction));
            }

            public Result(boolean disableButton, String alert) {
                this(disableButton, Optional.of(alert), Optional.empty());
            }
        }

        @FunctionalInterface
        public interface Handling {
            Result handle(TelegramApi api, CallbackMessage message);
        }

        @Override
        public void handle(TelegramApi api, Message message) {
            if (!(message instanceof CallbackMessage cm)) {
                return;
            }
            if (!cm.callback().command().equals(command)) {
                return;
            }
            var result = handler.handle(api, cm);

            result.alert.ifPresentOrElse( //
                    alert -> api.answer(cm.callbackId(), alert), //
                    () -> api.answer(cm.callbackId()));

            if (result.disableButton) {
                api.disableButton(cm.id());
            }

            result.reaction.ifPresent( //
                    reaction -> api.react(cm.id(), reaction));
        }
    }
}
