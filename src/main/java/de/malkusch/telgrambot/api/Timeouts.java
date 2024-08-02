package de.malkusch.telgrambot.api;

import java.time.Duration;

import static java.lang.Math.round;

public record Timeouts(Duration io, Duration polling) {

    public Timeouts(Duration io) {
        this(io, multiply(io, 10));
    }

    public Duration monitoring() {
        return polling(2);
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

    public Duration pinThrottle() {
        return polling(0.5);
    }

    public Duration updateSleep() {
        return io();
    }

    public Duration ping() {
        return polling(0.8);
    }

    private Duration polling(double factor) {
        return multiply(polling, factor);
    }

    private Duration io(double factor) {
        return multiply(io, factor);
    }

    private static Duration multiply(Duration duration, double factor) {
        return Duration.ofMillis(round(duration.toMillis() * factor));
    }
}
