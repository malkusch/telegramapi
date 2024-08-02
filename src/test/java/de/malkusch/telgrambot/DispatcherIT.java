package de.malkusch.telgrambot;

import de.malkusch.telgrambot.Handler.TextHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DispatcherIT {

    @RegisterExtension
    static final TelegramBotTestExtension telegram = new TelegramBotTestExtension();

    @Test
    @Timeout(30)
    public void shouldHandleCommand() throws InterruptedException {
        var expectation = new ExpectingHandler("ABBA");
        telegram.startDispatcher(
                expectation.textHandlers("A", "B", "unexpected")
        );
        Thread.sleep(200);

        telegram.send("unmapped");
        telegram.send("unmapped");
        telegram.send("unmapped");
        telegram.send("A");
        telegram.send("unmapped");
        telegram.send("B");
        telegram.send("unmapped");
        telegram.send("B");
        telegram.send("A");

        expectation.assertExpected();
    }

    private static class ExpectingHandler {

        public ExpectingHandler(String expectation) {
            this.expectation = expectation;
            semaphore = new CountDownLatch(expectation.length());
        }

        private final CountDownLatch semaphore;
        private final String expectation;
        private final StringBuffer commands = new StringBuffer();

        public void assertExpected() throws InterruptedException {
            semaphore.await();
            assertEquals(expectation, commands.toString());
        }

        public Handler textHandler(String command) {
            return new TextHandler(new Command(command), it -> {
                commands.append(command);
                semaphore.countDown();
            });
        }

        public Collection<Handler> textHandlers(String... commands) {
            return stream(commands).map(this::textHandler).toList();
        }
    }
}
