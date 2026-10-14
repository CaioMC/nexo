package io.github.caiomc.nexo.core.conversation.port.model.command;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;

/**
 * Abre a conversa com o modelo para um turno.
 *
 * @param history        mensagens finais dos turnos anteriores, mais antigas primeiro
 * @param preloadedTools funções enviadas já na 1ª chamada (memória de ferramentas ou PRESELECTED)
 * @param namespaces     namespaces dos contextos ligados, citados na descrição do {@code tool_search}
 */
public record OpenModelSessionCommand(
        String instructions,
        List<HistoryMessageCommand> history,
        String userMessage,
        ToolExposure exposure,
        List<Tool> preloadedTools,
        List<String> namespaces
) {

    public OpenModelSessionCommand {
        history = List.copyOf(history);
        preloadedTools = List.copyOf(preloadedTools);
        namespaces = List.copyOf(namespaces);
    }
}
