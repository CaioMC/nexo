package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.domain.enums.EventType;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.model.NewEvent;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.domain.model.Usage;
import io.github.caiomc.nexo.core.retrieval.domain.model.SelectedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.nonNull;

/**
 * Fábrica dos eventos da conversa, com o payload exato do contrato (doc 06, seção 4; asyncapi.yaml).
 */
public class ConversationEvents {

    public NewEvent messageUser(
            UUID turnId,
            String text,
            UUID clientMessageId
    ) {
        return new NewEvent(turnId, EventType.MESSAGE_USER, Map.of("text", text, "clientMessageId", clientMessageId.toString()));
    }

    public NewEvent turnStarted(
            UUID turnId,
            String model,
            Collection<ContextKey> contexts
    ) {
        return new NewEvent(turnId, EventType.TURN_STARTED, Map.of("model", model, "contexts", this.keys(contexts)));
    }

    public NewEvent assistantDelta(
            UUID turnId,
            String text
    ) {
        return new NewEvent(turnId, EventType.ASSISTANT_DELTA, Map.of("text", text));
    }

    public NewEvent assistantMessage(
            UUID turnId,
            String text
    ) {
        return new NewEvent(turnId, EventType.ASSISTANT_MESSAGE, Map.of("text", text));
    }

    public NewEvent toolSearch(
            UUID turnId,
            String goal,
            ToolSelection selection
    ) {
        List<Map<String, Object>> tools = selection.tools().stream().map(this::selected).toList();
        List<Map<String, Object>> blocked = selection.blocked().stream().map(this::blocked).toList();

        return this.event(
                turnId,
                EventType.TOOL_SEARCH,
                "goal", goal,
                "searchId", selection.searchId(),
                "strategy", selection.strategy().name(),
                "tools", tools,
                "blocked", blocked,
                "durationMs", selection.durationMs()
        );
    }

    public NewEvent contextSuggested(
            UUID turnId,
            List<BlockedTool> blocked
    ) {
        List<String> contexts = blocked.stream()
                .flatMap(tool -> tool.missingContexts().stream())
                .map(ContextKey::value)
                .distinct()
                .sorted()
                .toList();
        List<String> tools = blocked.stream().map(tool -> tool.name().value()).toList();

        return new NewEvent(turnId, EventType.CONTEXT_SUGGESTED, Map.of("contexts", contexts, "tools", tools));
    }

    public NewEvent toolStarted(
            UUID turnId,
            String callId,
            ToolName tool,
            String effect,
            Map<String, Object> arguments
    ) {
        return new NewEvent(turnId, EventType.TOOL_STARTED, Map.of("callId", callId, "tool", tool.value(), "effect", effect, "arguments", arguments));
    }

    public NewEvent toolBlocked(
            UUID turnId,
            String callId,
            ToolName tool,
            BlockReason reason,
            List<String> missing
    ) {
        return new NewEvent(turnId, EventType.TOOL_BLOCKED, Map.of("callId", callId, "tool", tool.value(), "reason", reason.name(), "missing", missing));
    }

    public NewEvent approvalRequested(ToolApproval approval) {
        return this.event(
                approval.turnId(),
                EventType.APPROVAL_REQUESTED,
                "approvalId", approval.id().toString(),
                "callId", approval.callId(),
                "tool", approval.tool().value(),
                "description", approval.description(),
                "severity", approval.severity().wire(),
                "arguments", approval.arguments(),
                "expiresAt", approval.expiresAt().toString()
        );
    }

    public NewEvent approvalResolved(
            UUID turnId,
            UUID approvalId,
            ApprovalStatus status
    ) {
        return new NewEvent(turnId, EventType.APPROVAL_RESOLVED, Map.of("approvalId", approvalId.toString(), "status", status.name()));
    }

    public NewEvent toolCompleted(
            UUID turnId,
            String callId,
            ToolName tool,
            boolean ok,
            String summary,
            String error,
            boolean truncated
    ) {
        return this.event(
                turnId,
                EventType.TOOL_COMPLETED,
                "callId", callId,
                "tool", tool.value(),
                "ok", ok,
                "summary", summary,
                "error", error,
                "truncated", truncated
        );
    }

    public NewEvent usageUpdated(
            UUID turnId,
            Usage usage
    ) {
        return new NewEvent(turnId, EventType.USAGE_UPDATED, this.usage(usage));
    }

    public NewEvent turnCompleted(
            UUID turnId,
            long durationMs,
            int toolCalls,
            Usage usage
    ) {
        return new NewEvent(turnId, EventType.TURN_COMPLETED, Map.of("durationMs", durationMs, "toolCalls", toolCalls, "usage", this.usage(usage)));
    }

    public NewEvent turnFailed(
            UUID turnId,
            FailureCode code
    ) {
        return new NewEvent(turnId, EventType.TURN_FAILED, Map.of("code", code.name(), "message", code.message()));
    }

    public NewEvent contextChanged(
            ContextKey context,
            boolean enabled
    ) {
        return new NewEvent(null, EventType.CONTEXT_CHANGED, Map.of("context", context.value(), "enabled", enabled));
    }

    public NewEvent conversationRenamed(String title) {
        return new NewEvent(null, EventType.CONVERSATION_RENAMED, Map.of("title", title));
    }

    private Map<String, Object> selected(SelectedTool tool) {
        Map<String, Object> item = new LinkedHashMap<>();

        item.put("step", tool.step());
        item.put("name", tool.name().value());
        item.put("origin", tool.origin().name());
        item.put("score", tool.score());
        item.put("level", tool.level());

        return item;
    }

    private Map<String, Object> blocked(BlockedTool tool) {
        Map<String, Object> item = new LinkedHashMap<>();

        item.put("name", tool.name().value());
        item.put("missingContexts", this.keys(tool.missingContexts()));

        return item;
    }

    private Map<String, Object> usage(Usage usage) {
        Map<String, Object> item = new LinkedHashMap<>();

        item.put("modelCalls", usage.modelCalls());
        item.put("inputTokens", usage.inputTokens());
        item.put("cachedInputTokens", usage.cachedInputTokens());
        item.put("outputTokens", usage.outputTokens());

        return item;
    }

    private List<String> keys(Collection<ContextKey> contexts) {
        return contexts.stream().map(ContextKey::value).sorted().toList();
    }

    /**
     * Payload com valores que podem faltar ({@code summary}, {@code error}, {@code searchId}): o
     * nulo fica de fora.
     */
    private NewEvent event(
            UUID turnId,
            EventType type,
            Object... pairs
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        for (int i = 0; i < pairs.length; i += 2) {
            if (nonNull(pairs[i + 1])) {
                payload.put((String) pairs[i], pairs[i + 1]);
            }
        }

        return new NewEvent(turnId, type, payload);
    }
}
