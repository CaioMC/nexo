package io.github.caiomc.nexo.core.conversation.port.model.data;

/**
 * O modelo pediu ferramentas: {@code tool_search_call} com o objetivo em linguagem natural.
 */
public record ToolSearchCallData(
        String callId,
        String goal
) {
}
