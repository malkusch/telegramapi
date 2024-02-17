package de.malkusch.telgrambot;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.UpdatesListener;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;

@Slf4j
final class ConnectionMonitoring implements AutoCloseable {

    private final TelegramApi api;
    private volatile Instant lastActivity = Instant.now();
    private volatile boolean connected = true;
    private final ScheduledExecutorService executor;

    public ConnectionMonitoring(TelegramApi api) {
        this.api = api;

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "telegram-monitor");
            t.setDaemon(true);
            return t;
        });
        start();
    }

    private void start() {
        var interval = api.timeouts.monitoring().toMillis();
        executor.scheduleAtFixedRate(this::checkConnection, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void checkConnection() {
        log.debug("check connection");
        var lastActivityDuration = Duration.between(lastActivity, Instant.now());
        if (lastActivityDuration.compareTo(api.timeouts.monitoring()) > 0) {
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
        executor.close();
    }
}
