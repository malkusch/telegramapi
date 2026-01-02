package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.TelegramApi;

import static java.util.Objects.requireNonNull;

public final class TelegramApiFactory {

    public static TelegramApi telegramApi(String chatId, String token, Timeouts timeouts) {
        requireNonNull(timeouts);

        requireNonNull(chatId);
        if (chatId.isBlank()) {
            throw new IllegalArgumentException("chatId must not be empty");
        }

        requireNonNull(token);
        if (token.isBlank()) {
            throw new IllegalArgumentException("token must not be empty");
        }

        InternalTelegramApi api = new TelegramHttpApi(chatId, token, timeouts);
        api = new TelegramRateLimitedApi(api, timeouts);
        api = new TelegramCircuitBreakerApi(api, timeouts);
        return api;
    }
}
