package de.malkusch.telgrambot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CountDownLatch;

import static de.malkusch.telgrambot.Handler.onCommand;
import static de.malkusch.telgrambot.Handler.onText;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReceiveUpdatesIT {

    @RegisterExtension
    static final TelegramBotTestExtension telegram = new TelegramBotTestExtension();

    @Test
    @Timeout(30)
    public void shouldHandleCommand() throws InterruptedException {
        var expectedText = new ExpectedUpdates("CCCACBCBA");
        var expectedCommands = new ExpectedUpdates("ABBA");
        telegram.receiveUpdates(
                onCommand("A", () -> expectedCommands.receive("A")), //
                onCommand("B", () -> expectedCommands.receive("B")), //
                onCommand("unexpected", () -> expectedCommands.receive("unexpected")), //
                onText(text -> expectedText.receive(text.message())) //
        );
        Thread.sleep(200);

        telegram.send("C");
        telegram.send("C");
        telegram.send("C");
        telegram.send("A");
        telegram.send("C");
        telegram.send("B");
        telegram.send("C");
        telegram.send("B");
        telegram.send("A");

        expectedText.assertUpdatesReceived();
        expectedCommands.assertUpdatesReceived();
    }

    private static class ExpectedUpdates {

        private final CountDownLatch semaphore;
        private final String expected;
        private final StringBuffer received = new StringBuffer();

        public ExpectedUpdates(String expected) {
            this.expected = expected;
            this.semaphore = new CountDownLatch(expected.length());
        }

        public void assertUpdatesReceived() throws InterruptedException {
            semaphore.await();
            assertEquals(expected, received.toString());
        }

        public void receive(String message) {
            received.append(message);
            semaphore.countDown();
        }
    }
}
