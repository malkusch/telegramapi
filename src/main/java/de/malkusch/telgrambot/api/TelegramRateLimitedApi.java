package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.UpdatesListener;
import de.malkusch.telgrambot.MessageId;
import de.malkusch.telgrambot.PinnedMessage;
import de.malkusch.telgrambot.UpdateReceiver;

import java.time.Duration;
import java.util.function.Function;

final class TelegramRateLimitedApi extends AbstractTelegramApiProxy {

    private final RateLimiter groupLimit;
    private final RateLimiter messageLimit;
    private final RateLimiter pinLimit;

    public TelegramRateLimitedApi(InternalTelegramApi api, Timeouts timeouts) {
        super(api);

        groupLimit = new RateLimiter(Duration.ofMinutes(1), 19, timeouts.groupThrottle(), "group");
        messageLimit = new RateLimiter(Duration.ofSeconds(1), 29, timeouts.messageThrottle(), "message");
        pinLimit = new RateLimiter(Duration.ofSeconds(2), 1, timeouts.pinThrottle(), "pin");
    }

    @Override
    public void receiveUpdates(Decorator<UpdatesListener> listenerDecorator, Decorator<ExceptionHandler> errorDecorator, UpdateReceiver... receivers) {
        api.receiveUpdates(listenerDecorator, errorDecorator, receivers);
    }

    @Override
    protected <R> R delegate(Function<InternalTelegramApi, R> call) {
        messageLimit.acquire();
        groupLimit.acquire();
        return super.delegate(call);
    }

    @Override
    public void pin(MessageId message) {
        pinLimit.acquire();
        super.pin(message);
    }

    @Override
    public PinnedMessage pinned() {
        pinLimit.acquire();
        return super.pinned();
    }

    @Override
    public void unpin() {
        pinLimit.acquire();
        super.unpin();
    }

    @Override
    public void unpin(MessageId message) {
        pinLimit.acquire();
        super.unpin(message);
    }

    @Override
    public void close() throws Exception {
        try (pinLimit; groupLimit; messageLimit) {
            super.close();
        }
    }
}
