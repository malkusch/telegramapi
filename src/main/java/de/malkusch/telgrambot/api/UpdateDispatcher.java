package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import de.malkusch.telgrambot.TelegramApi;
import de.malkusch.telgrambot.UpdateReceiver;

import java.util.List;

import static de.malkusch.telgrambot.api.UpdateFactory.update;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNull;

final class UpdateDispatcher implements ExceptionHandler, UpdatesListener {

    private static final System.Logger log = System.getLogger(UpdateDispatcher.class.getName());
    private final UpdateReceiver[] receivers;
    private final TelegramApi api;

    UpdateDispatcher(UpdateReceiver[] receivers, TelegramApi api) {
        this.receivers = requireNonNull(receivers);
        this.api = requireNonNull(api);
    }

    @Override
    public int process(List<Update> updates) {
        log.log(DEBUG, "Received {0} updates", updates.size());
        try {
            return updates.stream() //
                    .mapToInt(this::dispatch) //
                    .reduce((first, second) -> second) //
                    .orElse(CONFIRMED_UPDATES_ALL);

        } catch (Exception e) {
            log.log(WARNING, "Failed receive updates", e);
            return CONFIRMED_UPDATES_ALL;
        }
    }

    private int dispatch(Update apiUpdate) {
        var id = apiUpdate.updateId();
        var update = update(apiUpdate);
        for (var receiver : receivers) {
            try {
                receiver.receive(api, update);

            } catch (Exception e) {
                log.log(WARNING, "Failed receiving update " + update + " in receiver " + receiver, e);
            }
        }
        return id;
    }

    @Override
    public void onException(TelegramException e) {
        log.log(WARNING, "Telegram update failed", e);
    }
}
