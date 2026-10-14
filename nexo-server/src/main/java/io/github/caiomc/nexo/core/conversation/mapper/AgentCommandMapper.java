package io.github.caiomc.nexo.core.conversation.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.approval.command.DescribeApprovalCommand;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolOutcome;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolResultData;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.command.DisableContextCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;

import static java.util.Objects.isNull;

/**
 * Os comandos que o agente manda aos outros contextos (busca de ferramentas, contextos ligados) e
 * ao descritor de aprovações. O que vai ao modelo fica no {@link ModelCommandMapper}.
 */
@Component
public class AgentCommandMapper {

    /**
     * Sem objetivo informado pelo modelo, a busca usa a mensagem do usuário.
     */
    public SelectToolsCommand toSelectTools(
            String goal,
            Turn turn
    ) {
        return new SelectToolsCommand(
                isNull(goal) || goal.isBlank() ? turn.userMessage() : goal,
                null,
                null,
                null,
                turn.userId(),
                turn.id()
        );
    }

    public GetEnabledContextsCommand toEnabledContexts(UserId userId) {
        return new GetEnabledContextsCommand(userId);
    }

    public DisableContextCommand toDisableContext(
            UserId userId,
            ContextKey context
    ) {
        return new DisableContextCommand(userId, context.value());
    }

    public DescribeApprovalCommand toDescribeApproval(
            Tool tool,
            Map<String, Object> arguments
    ) {
        return new DescribeApprovalCommand(tool.name(), arguments);
    }

    /**
     * O executor estourou exceção: vira falha comum, que vai ao modelo como erro.
     */
    public ToolResultData toFailedResult(String message) {
        return new ToolResultData(
                ToolOutcome.FAILED,
                null,
                null,
                message,
                false
        );
    }
}
