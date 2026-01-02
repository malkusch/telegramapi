package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.UpdatesListener;
import de.malkusch.telgrambot.UpdateReceiver;

import java.util.function.Function;

final class TelegramCircuitBreakerApi extends AbstractTelegramApiProxy {

    private final CircuitBreaker circuitBreaker;

    public TelegramCircuitBreakerApi(InternalTelegramApi api, Timeouts timeouts) {
        super(api);
        circuitBreaker = new CircuitBreaker(timeouts);
    }

    @Override
    public void receiveUpdates(Decorator<UpdatesListener> listenerDecorator, Decorator<ExceptionHandler> errorDecorator, UpdateReceiver... receivers) {
        api.receiveUpdates(
                listenerDecorator.then(circuitBreaker::updatesListener),
                errorDecorator.then(circuitBreaker::exceptionHandler),
                receivers);
    }

    @Override
    protected <R> R delegate(Function<InternalTelegramApi, R> call) {
        return circuitBreaker.executeSupplier(() -> super.delegate(call));
    }

    CircuitBreaker circuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public void close() throws Exception {
        try (circuitBreaker) {
            super.close();
        }
    }
}
