package de.malkusch.telgrambot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CountDownLatch;

import static de.malkusch.telgrambot.Handler.onCommand;
import static de.malkusch.telgrambot.Handler.onText;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DispatcherIT {

    @RegisterExtension
    static final TelegramBotTestExtension telegram = new TelegramBotTestExtension();

    @Test
    @Timeout(30)
    public void shouldHandleCommand() throws InterruptedException {
        var expectation = new ExpectingHandler("ABBA", "CCCACBCBA");
        telegram.startDispatcher(
                expectation.commandHandler("A"), //
                expectation.commandHandler("B"), //
                expectation.commandHandler("unexpected"), //
                expectation.textHandler() //
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

        expectation.assertExpected();
    }

    private static class ExpectingHandler {

        public ExpectingHandler(String expectedCommands, String expectedText) {
            this.expectedCommands = expectedCommands;
            this.expectedText = expectedText;
            semaphore = new CountDownLatch(expectedCommands.length() + expectedText.length());
        }

        private final CountDownLatch semaphore;
        private final String expectedCommands;
        private final String expectedText;
        private final StringBuffer handledCommands = new StringBuffer();
        private final StringBuffer handledText = new StringBuffer();

        public void assertExpected() throws InterruptedException {
            semaphore.await();
            assertEquals(expectedCommands, handledCommands.toString());
            assertEquals(expectedText, handledText.toString());
        }

        public Handler commandHandler(String command) {
            return onCommand(command, () -> {
                handledCommands.append(command);
                semaphore.countDown();
            });

        }

        public Handler textHandler() {
            return onText((text) -> {
                handledText.append(text.message());
                semaphore.countDown();
            });
        }
    }
}
