package de.malkusch.telgrambot;

import org.junit.jupiter.api.extension.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

public class TelegramBotTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, ExecutionCondition {

    private final String chatId = System.getenv("TELEGRAM_CHAT_ID");
    private final String token = System.getenv("TELEGRAM_TOKEN");
    public TelegramApi api;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        var actor = System.getenv("GITHUB_TRIGGERING_ACTOR");
        if (actor != null && actor.equals("malkusch")) {
            return enabled("GITHUB_TRIGGERING_ACTOR=" + actor);
        }
        if (token != null) {
            return enabled("TELEGRAM_TOKEN given");
        }
        return disabled("GITHUB_TRIGGERING_ACTOR=" + actor);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        assertTrue(token != null && chatId != null);
        assertTrue(!token.isBlank() && !chatId.isBlank());

        api = new TelegramApi(chatId, token, Duration.ofSeconds(10));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        api.unpin();
        api.dropPendingUpdates();
    }

    private final List<MessageId> messages = new CopyOnWriteArrayList<>();

    public MessageId send(Function<TelegramApi, MessageId> send) {
        var message = send.apply(api);
        messages.add(message);
        return message;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try (var closing = api) {
            api.unpin();
            api.delete(messages);
            messages.clear();
            api.dropPendingUpdates();
        }
    }

    public PinnedMessage fetchMessage(MessageId messageId) {
        api.pin(messageId);
        try {
            return api.pinned();

        } finally {
            api.unpin(messageId);
        }
    }
}
