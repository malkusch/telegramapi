package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.UpdatesListener;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.lang.System.Logger;
import java.util.function.Supplier;

import static com.pengrad.telegrambot.UpdatesListener.CONFIRMED_UPDATES_ALL;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static java.lang.System.Logger.Level.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class CircuitBreaker implements AutoCloseable {

    public static final class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(Throwable cause) {
            super(cause);
        }
    }

    private static final Logger log = System.getLogger(CircuitBreaker.class.getName());
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private static final String CIRCUIT_BREAKER_NAME = "telegram";

    CircuitBreaker(Timeouts timeouts) {
        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .waitDurationInOpenState(timeouts.circuitBreaker())
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build();

        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME, circuitBreakerConfig);

        circuitBreaker.getEventPublisher().onStateTransition(it -> {
            switch (it.getStateTransition()) {
                case CLOSED_TO_METRICS_ONLY -> log.log(WARNING, "Circuit Breaker opened");
                case OPEN_TO_HALF_OPEN -> log.log(INFO, "Circuit Breaker half opened");
                case OPEN_TO_CLOSED, HALF_OPEN_TO_CLOSED -> log.log(INFO, "Circuit Breaker closed");
            }
        });
    }

    public UpdatesListener updatesListener(UpdatesListener updatesListener) {
        return list -> {
            try {
                return executeSupplier(() -> updatesListener.process(list), CONFIRMED_UPDATES_ALL);

            } catch (Throwable e) {
                log.log(WARNING, "Failed receive updates", e);
                return CONFIRMED_UPDATES_ALL;
            }
        };
    }

    public ExceptionHandler exceptionHandler(ExceptionHandler exceptionHandler) {
        return e -> error(e, () -> exceptionHandler.onException(e));
    }

    public void error(Throwable e, Runnable runnable) {
        circuitBreaker.onError(1, MILLISECONDS, e);
        if (circuitBreaker.tryAcquirePermission()) {
            runnable.run();
        }
    }

    public <T> T executeSupplier(Supplier<T> supplier, T fallback) {
        try {
            return executeSupplier(supplier);

        } catch (CircuitBreakerOpenException e) {
            log.log(DEBUG, "Circuit breaker open");
            return fallback;
        }
    }

    public <T> T executeSupplier(Supplier<T> supplier) {
        try {
            return circuitBreaker.executeSupplier(supplier);

        } catch (CallNotPermittedException e) {
            throw new CircuitBreakerOpenException(e);
        }
    }

    public boolean isOpen() {
        return circuitBreaker.getState() == OPEN;
    }

    public boolean isClosed() {
        return switch (circuitBreaker.getState()) {
            case CLOSED, HALF_OPEN -> true;
            default -> false;
        };
    }

    @Override
    public void close() {
        circuitBreakerRegistry.remove(CIRCUIT_BREAKER_NAME);
    }
}
