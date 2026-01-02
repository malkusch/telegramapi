package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import de.malkusch.telgrambot.api.CircuitBreaker.CircuitBreakerOpenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pengrad.telegrambot.UpdatesListener.CONFIRMED_UPDATES_ALL;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CircuitBreakerTest {

    private final Timeouts timeouts = new Timeouts(ofMillis(2));
    private final CircuitBreaker circuitBreaker = new CircuitBreaker(timeouts);

    static class TestException extends RuntimeException {
    }

    @BeforeEach
    public void silenceLogs() {
        getLogger(circuitBreaker.getClass().getName()).setLevel(SEVERE);
    }

    @AfterEach
    public void restoreLogs() {
        getLogger(circuitBreaker.getClass().getName()).setLevel(INFO);
    }

    @Test
    void errorShouldExecuteFirst() {
        AtomicInteger counter = new AtomicInteger();

        circuitBreaker.error(new RuntimeException(), counter::incrementAndGet);
        circuitBreaker.error(new RuntimeException(), counter::incrementAndGet);
        circuitBreaker.error(new RuntimeException(), counter::incrementAndGet);

        assertTrue(circuitBreaker.isClosed());
        assertEquals(3, counter.get());
    }

    @Test
    void executeSupplierFailureShouldExecuteFirst() {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            assertThrowsExactly(TestException.class, () -> circuitBreaker.executeSupplier(() -> {
                counter.incrementAndGet();
                throw new TestException();
            }));
        }

        assertTrue(circuitBreaker.isClosed());
        assertEquals(3, counter.get());
    }

    @Test
    void errorShouldOpen() {
        for (int i = 0; i < 200; i++) {
            circuitBreaker.error(new RuntimeException(), () -> {
            });
        }

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    void errorShouldNotExecuteWhenOpen() {
        AtomicInteger counter = new AtomicInteger();
        open();

        circuitBreaker.error(new RuntimeException(), counter::incrementAndGet);
        assertEquals(0, counter.get());
    }

    @Test
    void executeSupplierShouldOpen() {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 200; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException();
                });
            } catch (Throwable expected) {
            }
        }
        assertThrowsExactly(CircuitBreakerOpenException.class, () -> circuitBreaker.executeSupplier(counter::incrementAndGet));

        assertTrue(circuitBreaker.isOpen());
        assertEquals(0, counter.get());
    }

    @Test
    void executeSupplierShouldReturnFallback() {
        open();

        int result = circuitBreaker.executeSupplier(() -> 1, 2);

        assertEquals(2, result);
    }

    @Test
    void executeSupplierShouldNotOpen() {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 200; i++) {
            circuitBreaker.executeSupplier(counter::incrementAndGet);
        }

        assertTrue(circuitBreaker.isClosed());
        assertEquals(200, counter.get());
    }

    @Test
    void executeSupplierShouldCloseBack() throws Exception {
        open();

        MILLISECONDS.sleep(timeouts.circuitBreaker().toMillis() + 100);
        int result = circuitBreaker.executeSupplier(() -> 1, 0);

        assertTrue(circuitBreaker.isClosed());
        assertEquals(1, result);
    }

    @Test
    void executeSupplierShouldOpenBack() throws Exception {
        open();

        MILLISECONDS.sleep(timeouts.circuitBreaker().toMillis() + 100);
        for (int i = 0; i < 10; i++) {
            assertThrows(TestException.class, () -> circuitBreaker.executeSupplier(() -> {
                throw new TestException();
            }));
        }

        assertThrows(CircuitBreakerOpenException.class, () -> circuitBreaker.executeSupplier(() -> 1));
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    void updatesListenerShouldFallbackWhenOpened() {
        var delegate = mock(UpdatesListener.class);
        var listener = circuitBreaker.updatesListener(delegate);
        open();

        var result = listener.process(new ArrayList<>());

        verify(delegate, never()).process(anyList());
        assertEquals(CONFIRMED_UPDATES_ALL, result);
    }

    @Test
    void updatesListenerShouldOpen() {
        var list = new ArrayList<Update>();
        var listener = circuitBreaker.updatesListener(it -> {
            throw new TestException();
        });

        for (int i = 0; i < 200; i++) {
            listener.process(list);
        }

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    void exceptionHandlerShouldOpen() {
        var exception = new TelegramException(new RuntimeException());
        var handler = circuitBreaker.exceptionHandler(e -> {
        });

        for (int i = 0; i < 200; i++) {
            handler.onException(exception);
        }

        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    void exceptionHandlerShouldDoNothingWhenOpen() {
        var exception = new TelegramException(new RuntimeException());
        var delegate = mock(ExceptionHandler.class);
        var handler = circuitBreaker.exceptionHandler(delegate);
        open();

        handler.onException(exception);

        verify(delegate, never()).onException(any());
    }

    private void open() {
        var exception = new TestException();
        for (int i = 0; i < 200; i++) {
            circuitBreaker.error(exception, () -> {
            });
        }
    }
}
