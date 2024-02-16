package de.malkusch.telgrambot;

import static com.pengrad.telegrambot.UpdatesListener.CONFIRMED_UPDATES_ALL;
import static de.malkusch.telgrambot.MessageFactory.message;

import java.util.Collection;
import java.util.List;

import com.pengrad.telegrambot.model.Update;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class MessageDispatcher {

    private final Collection<Handler> handlers;
    private final TelegramApi api;

    int dispatch(List<Update> updates) {
        return updates.stream() //
                .mapToInt(this::dispatch) //
                .reduce((first, second) -> second) //
                .orElse(CONFIRMED_UPDATES_ALL);
    }

    private int dispatch(Update update) {
        var id = update.updateId();
        var message = message(update);
        handlers.forEach(it -> it.handle(api, message));
        return id;
    }
}
