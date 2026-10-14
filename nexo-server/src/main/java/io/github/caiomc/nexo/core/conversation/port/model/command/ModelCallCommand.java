package io.github.caiomc.nexo.core.conversation.port.model.command;

import java.util.List;

/**
 * Uma chamada ao modelo, levando as respostas aos pedidos da chamada anterior (vazia na 1ª).
 *
 * @param toolOutputs uma resposta por {@code tool_search_call} ou {@code function_call}, no mesmo {@code call_id}
 */
public record ModelCallCommand(List<ToolOutputCommand> toolOutputs) {

    public ModelCallCommand {
        toolOutputs = List.copyOf(toolOutputs);
    }
}
