package de.malkusch.telgrambot;

import de.malkusch.telgrambot.UpdateReceiver.CallbackReceiver.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CountDownLatch;

import static de.malkusch.telgrambot.Reaction.THUMBS_UP;
import static de.malkusch.telgrambot.UpdateReceiver.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReceiveUpdatesIT {

    @RegisterExtension
    static final TelegramBotTestExtension telegram = new TelegramBotTestExtension();

    @Test
    @Timeout(30)
    public void shouldHandleCommand() throws InterruptedException {
        var expectedText = new ExpectedUpdates("CCCACBCBA");
        var expectedText2 = new ExpectedUpdates("cccacbcba");
        var expectedCommands = new ExpectedUpdates("ABBA");
        var expectedReactions = new ExpectedUpdates("");
        var expectedCallbacks = new ExpectedUpdates("");

        telegram.receiveUpdates(
                onCommand("A", () -> expectedCommands.receive("A")), //
                onCommand("B", () -> expectedCommands.receive("B")), //
                onCommand("unexpected", () -> expectedCommands.receive("unexpectedCommand")), //

                onReaction(THUMBS_UP, update -> expectedReactions.receive("unexpectedReaction")), //

                onCallback("A", callback -> {
                    expectedCallbacks.receive("unexpectedCallback");
                    return new Result(false);
                }), //

                onText(text -> expectedText.receive(text.message())), //
                onText(text -> expectedText2.receive(text.message().toLowerCase())) //
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
        expectedText2.assertUpdatesReceived();
        expectedCommands.assertUpdatesReceived();
        expectedReactions.assertUpdatesReceived();
        expectedCallbacks.assertUpdatesReceived();
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
