package de.malkusch.telgrambot;

import static de.malkusch.telgrambot.MessageFactory.message;

import java.util.Collection;
import java.util.List;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
final class MessageDispatcher implements ExceptionHandler, UpdatesListener {

    private final Collection<Handler> handlers;
    private final TelegramApi api;

    @Override
    public int process(List<Update> updates) {
        log.debug("Received {} updates", updates.size());
        return updates.stream() //
                .mapToInt(this::dispatch) //
                .reduce((first, second) -> second) //
                .orElse(CONFIRMED_UPDATES_ALL);
    }

    private int dispatch(Update update) {
        var id = update.updateId();
        var message = message(update);
        for (var handler : handlers) {
            try {
                handler.handle(api, message);

            } catch (Exception e) {
                log.warn("Couldn't handle message {}", message, e);
            }
        }
        return id;
    }

    @Override
    public void onException(TelegramException e) {
        log.warn("Telegram update failed", e);
    }
}
