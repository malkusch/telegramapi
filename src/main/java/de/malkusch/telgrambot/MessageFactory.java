package de.malkusch.telgrambot;

import static de.malkusch.telgrambot.Message.ReactionMessage.Reaction.IGNORED;
import static de.malkusch.telgrambot.Message.ReactionMessage.Reaction.THUMBS_UP;
import static java.util.Arrays.stream;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.reaction.ReactionType;
import com.pengrad.telegrambot.model.reaction.ReactionTypeEmoji;

import de.malkusch.telgrambot.Message.CallbackMessage;
import de.malkusch.telgrambot.Message.CallbackMessage.Callback;
import de.malkusch.telgrambot.Message.ReactionMessage;
import de.malkusch.telgrambot.Message.ReactionMessage.Reaction;
import de.malkusch.telgrambot.Message.TextMessage;
import de.malkusch.telgrambot.Message.UnknownMessage;

final class MessageFactory {

    static Message message(Update update) {
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
            return new ReactionMessage(id, reactions, fromBot);
        }

        if (update.callbackQuery() != null) {
            var callbackId = new CallbackMessage.CallbackId(update.callbackQuery().id());
            var data = update.callbackQuery().data();
            var callback = Callback.parse(data);
            var id = new MessageId(update.callbackQuery().message().messageId());
            return new CallbackMessage(id, callbackId, callback);
        }

        return new UnknownMessage();
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
