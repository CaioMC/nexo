package io.github.caiomc.nexo.core.conversation.port.tool;

import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolResultData;

/**
 * As mãos do agente: chama a API do Google. Não decide nada (quem decide é o gatekeeper).
 */
public interface ToolExecutorPort {

    ToolResultData execute(ExecuteToolCommand command);
}
