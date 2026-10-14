package io.github.caiomc.nexo.core.conversation.port.model.command;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;

/**
 * O que o servidor responde a um pedido do modelo, no mesmo {@code call_id}.
 */
public sealed interface ToolOutputCommand {

    String callId();

    /**
     * Resposta a um {@code tool_search_call}: as definições escolhidas pelo RAG + DAG, na ordem de execução.
     */
    record SearchResult(
            String callId,
            List<Tool> tools
    ) implements ToolOutputCommand {

        public SearchResult {
            tools = List.copyOf(tools);
        }
    }

    /**
     * Resposta a um {@code function_call}: a saída da ferramenta (ou o motivo de não ter rodado), em JSON.
     */
    record FunctionResult(
            String callId,
            String output
    ) implements ToolOutputCommand {
    }
}
