package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.model.ChatFullInfo;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import de.malkusch.telgrambot.MessageId;
import de.malkusch.telgrambot.PinnedMessage;
import de.malkusch.telgrambot.PinnedMessage.CallbackMessage;
import de.malkusch.telgrambot.PinnedMessage.CallbackMessage.Button;
import de.malkusch.telgrambot.PinnedMessage.TextMessage;

import java.util.Arrays;

import static de.malkusch.telgrambot.PinnedMessage.NO_MESSAGE;
import static java.util.Optional.ofNullable;

final class PinnedMessageFactory {
    public static PinnedMessage pinnedMessage(ChatFullInfo chat) {
        return ofNullable(chat) //
                .map(ChatFullInfo::pinnedMessage) //

                .flatMap(pinnedMessage -> {
                    var id = new MessageId(pinnedMessage.messageId());
                    // var fromBot = it.viaBot() != null || it.from() != null && it.from().isBot();
                    var text = ofNullable(pinnedMessage.text());

                    var buttons = ofNullable(pinnedMessage.replyMarkup()) //
                            .map(InlineKeyboardMarkup::inlineKeyboard).stream() //
                            .flatMap(Arrays::stream).flatMap(Arrays::stream) //
                            .flatMap(it -> ofNullable(it.text())
                                    .flatMap(name -> ofNullable(it.callbackData())
                                            .map(cb -> new Button(name, cb))).stream()) //
                            .toList();

                    if (buttons.isEmpty()) {
                        return text.map(it -> new TextMessage(id, it));

                    } else {
                        return text.map(it -> new CallbackMessage(id, it, buttons));
                    }

                }) //

                .orElse(NO_MESSAGE);
    }

}
