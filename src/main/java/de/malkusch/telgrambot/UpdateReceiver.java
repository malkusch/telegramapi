package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Update.CallbackUpdate;
import de.malkusch.telgrambot.Update.ReactionUpdate;
import de.malkusch.telgrambot.Update.TextMessage;

import java.util.Optional;

public interface UpdateReceiver {

    void receive(TelegramApi api, Update update);

    @FunctionalInterface
    interface TextReceiver {
        void receive(TextMessage message);
    }

    static UpdateReceiver onText(TextReceiver receiver) {
        return (api, update) -> {
            if (!(update instanceof TextMessage text)) {
                return;
            }
            if (text.fromBot()) {
                return;
            }
            receiver.receive(text);
        };
    }

    @FunctionalInterface
    interface CommandReceiver {
        void receive();
    }

    static UpdateReceiver onCommand(String command, CommandReceiver receiver) {
        return onCommand(new Command(command), receiver);
    }

    static UpdateReceiver onCommand(Command command, CommandReceiver receiver) {
        return onText(text -> {
            if (text.message().equalsIgnoreCase(command.name())) {
                receiver.receive();
            }
        });
    }

    @FunctionalInterface
    interface ReactionReceiver {
        void receive(ReactionUpdate update);
    }

    static UpdateReceiver onReaction(Reaction reaction, ReactionReceiver receiver) {
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
            receiver.receive(reactionUpdate);
        };
    }

    @FunctionalInterface
    interface CallbackReceiver {
        CallbackReceiver.Result receive(CallbackUpdate update);

        record Result(boolean disableButton, Optional<String> alert, Optional<Reaction> reaction) {

            public Result(boolean disableButton) {
                this(disableButton, Optional.empty(), Optional.empty());
            }

            public Result(boolean disableButton, Reaction reaction) {
                this(disableButton, Optional.empty(), Optional.of(reaction));
            }

            public Result(boolean disableButton, String alert) {
                this(disableButton, Optional.of(alert), Optional.empty());
            }
        }
    }

    static UpdateReceiver onCallback(String command, CallbackReceiver receiver) {
        return onCallback(new Command(command), receiver);
    }

    static UpdateReceiver onCallback(Command command, CallbackReceiver receiver) {
        return (api, update) -> {
            if (!(update instanceof CallbackUpdate callbackUpdate)) {
                return;
            }
            if (!callbackUpdate.callback().command().equals(command)) {
                return;
            }
            var result = receiver.receive(callbackUpdate);

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
