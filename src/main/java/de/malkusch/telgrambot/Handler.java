package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Message.CallbackMessage;
import de.malkusch.telgrambot.Message.ReactionMessage;
import de.malkusch.telgrambot.Message.ReactionMessage.Reaction;
import de.malkusch.telgrambot.Message.TextMessage;

import java.util.Optional;

public interface Handler {

    void handle(TelegramApi api, Message message);

    @FunctionalInterface
    interface TextHandler {
        void handle(TextMessage message);
    }

    static Handler onText(TextHandler handler) {
        return (api, message) -> {
            if (!(message instanceof TextMessage text)) {
                return;
            }
            if (text.fromBot()) {
                return;
            }
            handler.handle(text);
        };
    }

    @FunctionalInterface
    interface CommandHandler {
        void handle();
    }

    static Handler onCommand(String command, CommandHandler handler) {
        return onCommand(new Command(command), handler);
    }

    static Handler onCommand(Command command, CommandHandler handler) {
        return onText(text -> {
            if (text.message().equalsIgnoreCase(command.name())) {
                handler.handle();
            }
        });
    }

    @FunctionalInterface
    interface ReactionHandler {
        void handle(ReactionMessage message);
    }

    static Handler onReaction(Reaction reaction, ReactionHandler handler) {
        return (api, message) -> {
            if (!(message instanceof ReactionMessage reactionMessage)) {
                return;
            }
            if (reactionMessage.fromBot()) {
                return;
            }
            if (!reactionMessage.contains(reaction)) {
                return;
            }
            handler.handle(reactionMessage);
        };
    }

    @FunctionalInterface
    interface CallbackHandler {
        CallbackHandler.Result handle(CallbackMessage message);

        record Result(boolean disableButton, Optional<String> alert,
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
    }

    static Handler onCallback(Command command, CallbackHandler handler) {
        return (api, message) -> {
            if (!(message instanceof CallbackMessage callbackMessage)) {
                return;
            }
            if (!callbackMessage.callback().command().equals(command)) {
                return;
            }
            var result = handler.handle(callbackMessage);

            result.alert.ifPresentOrElse( //
                    alert -> api.answer(callbackMessage.callbackId(), alert), //
                    () -> api.answer(callbackMessage.callbackId()));

            if (result.disableButton) {
                api.disableButton(callbackMessage.id());
            }

            result.reaction.ifPresent( //
                    reaction -> api.react(callbackMessage.id(), reaction));
        };
    }
}
