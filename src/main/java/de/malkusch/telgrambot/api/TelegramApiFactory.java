package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.TelegramApi;

public final class TelegramApiFactory {

    public static TelegramApi telegramApi(String chatId, String token, Timeouts timeouts) {
        TelegramApi api = new TelegramHttpApi(chatId, token, timeouts);
        api = new TelegramRateLimitedApi(api, timeouts);
        return api;
    }
}
