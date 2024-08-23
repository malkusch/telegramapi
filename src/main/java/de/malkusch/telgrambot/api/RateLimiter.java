package de.malkusch.telgrambot.api;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.time.Duration;

import static de.malkusch.telgrambot.api.Timeouts.assertPositive;
import static java.util.Objects.requireNonNull;

final class RateLimiter implements AutoCloseable {

    private final io.github.resilience4j.ratelimiter.RateLimiter limiter;

    RateLimiter(Duration period, int limit, Duration throttle, String name) {
        assertPositive(period, "period");
        assertPositive(throttle, "throttle");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        requireNonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }

        var config = RateLimiterConfig.custom()
                .timeoutDuration(throttle)
                .limitRefreshPeriod(period)
                .limitForPeriod(limit)
                .build();

        var registry = RateLimiterRegistry.of(config);
        limiter = registry.rateLimiter(name);

        close = () -> {
            registry.remove(limiter.getName());
            registry.removeConfiguration(limiter.getName());
        };
    }

    public void acquire() {
        if (!limiter.acquirePermission()) {
            throw new RuntimeException("Rate limiting " + this);
        }
    }

    @Override
    public String toString() {
        return limiter.getName();
    }

    private final AutoCloseable close;

    @Override
    public void close() throws Exception {
        close.close();
    }
}
