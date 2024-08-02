package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Update.CallbackUpdate;
import de.malkusch.telgrambot.Update.ReactionUpdate;
import de.malkusch.telgrambot.Update.ReactionUpdate.Reaction;
import de.malkusch.telgrambot.Update.TextMessage;

import java.util.Optional;

public interface Handler {

    void handle(TelegramApi api, Update update);

    @FunctionalInterface
    interface TextHandler {
        void handle(TextMessage message);
    }

    static Handler onText(TextHandler handler) {
        return (api, update) -> {
            if (!(update instanceof TextMessage text)) {
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
        void handle(ReactionUpdate update);
    }

    static Handler onReaction(Reaction reaction, ReactionHandler handler) {
        return (api, update) -> {
            if (!(update instanceof ReactionUpdate reactionUpdate)) {
                return;
            }
            if (reactionUpdate.fromBot()) {
                return;
            }
            if (!reactionUpdate.contains(reaction)) {
                return;
            }
            handler.handle(reactionUpdate);
        };
    }

    @FunctionalInterface
    interface CallbackHandler {
        CallbackHandler.Result handle(CallbackUpdate callback);

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
        return (api, update) -> {
            if (!(update instanceof CallbackUpdate callbackUpdate)) {
                return;
            }
            if (!callbackUpdate.callback().command().equals(command)) {
                return;
            }
            var result = handler.handle(callbackUpdate);

            result.alert.ifPresentOrElse( //
                    alert -> api.answer(callbackUpdate.callbackId(), alert), //
                    () -> api.answer(callbackUpdate.callbackId()));

            if (result.disableButton) {
                api.disableButton(callbackUpdate.id());
            }

            result.reaction.ifPresent( //
                    reaction -> api.react(callbackUpdate.id(), reaction));
        };
    }
}
