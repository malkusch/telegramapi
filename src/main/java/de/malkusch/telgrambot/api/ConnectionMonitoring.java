package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.UpdatesListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
final class ConnectionMonitoring implements AutoCloseable {

    private final Timeouts timeouts;
    private volatile Instant lastActivity = Instant.now();
    private volatile boolean connected = true;
    private final ScheduledExecutorService executor;

    public ConnectionMonitoring(Timeouts timeouts) {
        this.timeouts = timeouts;

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "telegram-monitor");
            t.setDaemon(true);
            return t;
        });
    }

    private volatile boolean started = false;
    private final Object lock = new Object();

    public void startMonitoring() {
        synchronized (lock) {
            if (started) {
                return;
            }
            started = true;
        }
        log.info("start monitoring of the telegram connection");
        var interval = timeouts.monitoring().toMillis();
        executor.scheduleAtFixedRate(this::checkConnection, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void checkConnection() {
        log.debug("check connection");
        var lastActivityDuration = Duration.between(lastActivity, Instant.now());
        if (lastActivityDuration.compareTo(timeouts.monitoring()) > 0) {
            disconnected();
        }
    }

    ExceptionHandler exceptionHandler(ExceptionHandler handler) {
        return (e -> {
            var response = e.response();
            if (response == null && e.getCause() instanceof IOException) {
                log.debug("Disconnected from IOException");
                disconnected();
            }
            handler.onException(e);
        });
    }

    UpdatesListener updateListener(UpdatesListener updateListener) {
        return (it -> {
            log.debug("update activity");
            registerActivity();
            return updateListener.process(it);
        });
    }

    Interceptor interceptor() {
        return it -> {
            log.debug("http activity");
            var request = it.request();
            var response = it.proceed(request);
            if (response.isSuccessful()) {
                registerActivity();
            }
            return response;
        };
    }

    private void registerActivity() {
        lastActivity = Instant.now();
        if (!connected) {
            reconnected();
        }
    }

    private void reconnected() {
        connected = true;
        log.info("Telegram connection is back again");
    }

    private void disconnected() {
        if (!connected) {
            return;
        }
        connected = false;
        log.info("Telegram connection is down");
    }

    @Override
    public void close() throws Exception {
        synchronized (lock) {
            if (!started) {
                return;
            }
        }
        log.info("stop monitoring of the telegram connection");

        executor.shutdown();
        var timeout = timeouts.monitoring().toMillis();
        if (executor.awaitTermination(timeout, MILLISECONDS)) {
            return;
        }
        log.warn("Failed shutting down scheduler. Forcing shutdown now!");
        executor.shutdownNow();
        if (!executor.awaitTermination(timeout, MILLISECONDS)) {
            log.error("Forced shutdown failed");
        }
    }
}
