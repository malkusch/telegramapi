package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.reaction.ReactionType;
import com.pengrad.telegrambot.model.reaction.ReactionTypeEmoji;
import de.malkusch.telgrambot.Callback;
import de.malkusch.telgrambot.MessageId;
import de.malkusch.telgrambot.Reaction;
import de.malkusch.telgrambot.Update;
import de.malkusch.telgrambot.Update.CallbackUpdate;
import de.malkusch.telgrambot.Update.ReactionUpdate;
import de.malkusch.telgrambot.Update.TextMessage;
import de.malkusch.telgrambot.Update.UnknownUpdate;

import static de.malkusch.telgrambot.Reaction.UNKNOWN;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

final class UpdateFactory {

    static Update update(com.pengrad.telegrambot.model.Update update) {
        if (update.message() != null && update.message().text() != null) {
            var id = new MessageId(update.message().messageId());
            var fromBot = update.message().from().isBot();
            return new TextMessage(id, update.message().text(), fromBot);
        }

        if (update.messageReaction() != null) {
            var id = new MessageId(update.messageReaction().messageId());
            var reactions = stream(update.messageReaction().newReaction()) //
                    .map(UpdateFactory::reaction) //
                    .toList();
            var fromBot = update.messageReaction().user().isBot();
            return new ReactionUpdate(id, reactions, fromBot);
        }

        if (update.callbackQuery() != null) {
            var callbackId = new CallbackUpdate.CallbackId(update.callbackQuery().id());
            var data = update.callbackQuery().data();
            var callback = Callback.parse(data);

            return ofNullable(update.callbackQuery().maybeInaccessibleMessage()) //
                    .map(MaybeInaccessibleMessage::messageId) //
                    .map(MessageId::new) //
                    .map(id -> (Update) new CallbackUpdate(id, callbackId, callback)) //
                    .orElse(new UnknownUpdate());
        }

        return new UnknownUpdate();
    }

    private static Reaction reaction(ReactionType reaction) {
        if (reaction instanceof ReactionTypeEmoji emoji) {
            return ofNullable(emoji.emoji()) //
                    .map(Reaction::new) //
                    .orElse(UNKNOWN);
        }
        return UNKNOWN;
    }
}
