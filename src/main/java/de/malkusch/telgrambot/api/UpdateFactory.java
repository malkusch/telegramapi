package de.malkusch.telgrambot.api;

import static de.malkusch.telgrambot.Update.ReactionUpdate.Reaction.IGNORED;
import static de.malkusch.telgrambot.Update.ReactionUpdate.Reaction.THUMBS_UP;
import static java.util.Arrays.stream;

import com.pengrad.telegrambot.model.reaction.ReactionType;
import com.pengrad.telegrambot.model.reaction.ReactionTypeEmoji;

import de.malkusch.telgrambot.Update;
import de.malkusch.telgrambot.Update.CallbackUpdate;
import de.malkusch.telgrambot.Update.CallbackUpdate.Callback;
import de.malkusch.telgrambot.Update.ReactionUpdate;
import de.malkusch.telgrambot.Update.ReactionUpdate.Reaction;
import de.malkusch.telgrambot.Update.TextMessage;
import de.malkusch.telgrambot.Update.UnknownUpdate;
import de.malkusch.telgrambot.MessageId;

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
                    .map(it -> reaction(it)) //
                    .toList();
            var fromBot = update.messageReaction().user().isBot();
            return new ReactionUpdate(id, reactions, fromBot);
        }

        if (update.callbackQuery() != null) {
            var callbackId = new CallbackUpdate.CallbackId(update.callbackQuery().id());
            var data = update.callbackQuery().data();
            var callback = Callback.parse(data);
            var id = new MessageId(update.callbackQuery().message().messageId());
            return new CallbackUpdate(id, callbackId, callback);
        }

        return new UnknownUpdate();
    }

    private static Reaction reaction(ReactionType reaction) {
        if (reaction instanceof ReactionTypeEmoji emoji) {
            var value = emoji.emoji();
            if (value == null) {
                return IGNORED;

            } else if (value.equals("👍")) {
                return THUMBS_UP;
            }
        }
        return IGNORED;
    }
}
