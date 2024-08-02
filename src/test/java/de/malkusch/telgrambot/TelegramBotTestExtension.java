package de.malkusch.telgrambot;

import de.malkusch.telgrambot.api.AbstractTelegramApiProxy;
import org.junit.jupiter.api.extension.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static de.malkusch.telgrambot.TelegramApi.telegramApi;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

public class TelegramBotTestExtension extends AbstractTelegramApiProxy implements BeforeAllCallback, AfterEachCallback, AfterAllCallback, ExecutionCondition {

    private static final String CHAT_ID = System.getenv("TELEGRAM_CHAT_ID");
    private static final String TOKEN = System.getenv("TELEGRAM_TOKEN");

    public TelegramBotTestExtension() {
        super(telegramApi(CHAT_ID, TOKEN, Duration.ofSeconds(10)));
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        var actor = System.getenv("GITHUB_TRIGGERING_ACTOR");
        if (actor != null && actor.equals("malkusch")) {
            return enabled("GITHUB_TRIGGERING_ACTOR=" + actor);
        }
        if (TOKEN != null) {
            return enabled("TELEGRAM_TOKEN given");
        }
        return disabled("GITHUB_TRIGGERING_ACTOR=" + actor);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        assertTrue(TOKEN != null && CHAT_ID != null);
        assertTrue(!TOKEN.isBlank() && !CHAT_ID.isBlank());

        unpin();
        dropPendingUpdates();
    }

    private final Set<MessageId> pinned = new HashSet<>();

    @Override
    public void pin(MessageId message) {
        super.pin(message);
        pinned.add(message);
    }

    @Override
    public void unpin() {
        super.unpin();
        pinned.clear();
    }

    @Override
    public void unpin(MessageId message) {
        super.unpin(message);
        pinned.remove(message);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (!pinned.isEmpty()) {
            unpin();
        }
        dropPendingUpdates();
    }

    private final List<MessageId> messages = new CopyOnWriteArrayList<>();

    @Override
    public MessageId send(String text) {
        var message = super.send(text);
        messages.add(message);
        return message;
    }

    @Override
    public MessageId send(String text, Button... buttons) {
        var message = super.send(text, buttons);
        messages.add(message);
        return message;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try (this) {
            delete(messages);
            messages.clear();
            dropPendingUpdates();
        }
    }

    public PinnedMessage fetchMessage(MessageId messageId) {
        pin(messageId);
        try {
            return pinned();

        } finally {
            unpin(messageId);
        }
    }
}
