package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.api.CircuitBreaker.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TelegramCircuitBreakerApiTest {

    @Mock
    private InternalTelegramApi delegate;
    private TelegramCircuitBreakerApi api;

    @BeforeEach
    public void setup() {
        api = new TelegramCircuitBreakerApi(delegate, new Timeouts(ofMillis(2)));
    }

    @Test
    public void sendShouldDelegateWhenClosed() {
        api.send("Any");
        verify(delegate).send("Any");
    }

    @Test
    public void sendShouldNotDelegateWhenOpen() {
        open();
        assertThrows(CircuitBreakerOpenException.class, () -> api.send("Any"));
        verify(delegate, never()).send(anyString());
    }

    private void open() {
        for (int i = 0; i < 200; i++) {
            api.circuitBreaker().error(new RuntimeException(), () -> {
            });
        }
    }
}
