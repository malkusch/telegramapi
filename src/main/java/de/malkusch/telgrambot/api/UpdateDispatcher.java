package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import de.malkusch.telgrambot.UpdateReceiver;
import de.malkusch.telgrambot.TelegramApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static de.malkusch.telgrambot.api.UpdateFactory.update;

@RequiredArgsConstructor
@Slf4j
final class UpdateDispatcher implements ExceptionHandler, UpdatesListener {

    private final UpdateReceiver[] receivers;
    private final TelegramApi api;

    @Override
    public int process(List<Update> updates) {
        log.debug("Received {} updates", updates.size());
        return updates.stream() //
                .mapToInt(this::dispatch) //
                .reduce((first, second) -> second) //
                .orElse(CONFIRMED_UPDATES_ALL);
    }

    private int dispatch(Update apiUpdate) {
        var id = apiUpdate.updateId();
        var update = update(apiUpdate);
        for (var handler : receivers) {
            try {
                handler.receive(api, update);

            } catch (Exception e) {
                log.warn("Couldn't handle update {}", update, e);
            }
        }
        return id;
    }

    @Override
    public void onException(TelegramException e) {
        log.warn("Telegram update failed", e);
    }
}
