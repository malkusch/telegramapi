package de.malkusch.telgrambot;

import com.pengrad.telegrambot.model.ChatFullInfo;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public sealed interface PinnedMessage {

    record TextMessage(MessageId id, String text) implements PinnedMessage {
    }

    record CallbackMessage(MessageId id, String text, List<Button> buttons) implements PinnedMessage {

        CallbackMessage(MessageId id, String text, Button... buttons) {
            this(id, text, asList(buttons));
        }

        CallbackMessage(MessageId id, String text, TelegramApi.Button... buttons) {
            this(id, text, stream(buttons).map(Button::new).toList());
        }

        record Button(String name, String callback) {

            Button( TelegramApi.Button button) {
                this(button.name(), button.callback().toString());
            }

        }
    }

    public static NoMessage NO_MESSAGE = new NoMessage();

    record NoMessage() implements PinnedMessage {

    }

    static PinnedMessage pinnedMessage(ChatFullInfo chat) {
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
                                            .map(cb -> new CallbackMessage.Button(name, cb))).stream()) //
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
