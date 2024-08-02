package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.TelegramApi;

import java.time.Duration;

public final class TelegramApiFactory {

    public static TelegramApi telegramApi(String chatId, String token, Duration timeout) {
        var timeouts = new Timeouts(timeout);
        TelegramApi api = new TelegramHttpApi(chatId, token, timeouts);
        api = new TelegramRateLimitedApi(api, timeouts);
        return api;
    }

}
