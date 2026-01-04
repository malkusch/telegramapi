package de.malkusch.telgrambot.api;

import java.time.Duration;

import static java.lang.Math.round;
import static java.time.Duration.ZERO;
import static java.util.Objects.requireNonNull;

public record Timeouts(Duration io, Duration polling, Duration updateSleep) {

    public Timeouts {
        assertPositive(io, "io timeout");
        assertPositive(polling, "polling timeout");
    }

    static void assertPositive(Duration duration, String name) {
        requireNonNull(duration);
        if (duration.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public Timeouts(Duration io, Duration polling) {
        this(io, polling, ZERO);
    }

    public Timeouts(Duration io) {
        this(io, multiply(io, 10));
    }

    public Duration circuitBreaker() {
        return polling(10);
    }

    public Duration monitoring() {
        return polling(5);
    }

    public Duration keepAlive() {
        return polling(2.2);
    }

    public Duration call() {
        return polling(1.2);
    }

    public Duration groupThrottle() {
        return polling(1.1);
    }

    public Duration messageThrottle() {
        return polling(1.1);
    }

    public Duration ping() {
        return polling(0.8);
    }

    public Duration pinThrottle() {
        return polling(0.5);
    }

    private Duration polling(double factor) {
        return multiply(polling, factor);
    }

    private static Duration multiply(Duration duration, double factor) {
        return Duration.ofMillis(round(duration.toMillis() * factor));
    }
}
