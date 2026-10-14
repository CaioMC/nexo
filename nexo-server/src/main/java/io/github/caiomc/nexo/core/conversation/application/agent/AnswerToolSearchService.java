package io.github.caiomc.nexo.core.conversation.application.agent;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.AgentCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ModelCommandMapper;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;
import io.github.caiomc.nexo.core.conversation.port.model.data.ToolSearchCallData;
import io.github.caiomc.nexo.core.retrieval.domain.model.SelectedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.usecase.SelectToolsUseCase;

/**
 * Responde o {@code tool_search_call} do modelo: o pipeline RAG + DAG (contexto retrieval) com os
 * contextos ligados agora. As definições encontradas voltam ao modelo; ferramentas relevantes de
 * contexto desligado viram {@code context.suggested} e não vão ao modelo.
 */
@Service
public class AnswerToolSearchService {

    private final SelectToolsUseCase selectTools;
    private final ProvideCatalogUseCase catalog;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;

    private final AgentCommandMapper agentCommands;
    private final ModelCommandMapper modelCommands;

    public AnswerToolSearchService(
            SelectToolsUseCase selectTools,
            ProvideCatalogUseCase catalog,
            ConversationEventHubService hub,
            ConversationEvents events,
            AgentCommandMapper agentCommands,
            ModelCommandMapper modelCommands
    ) {
        this.selectTools = selectTools;
        this.catalog = catalog;
        this.hub = hub;
        this.events = events;
        this.agentCommands = agentCommands;
        this.modelCommands = modelCommands;
    }

    /**
     * @return a resposta ao modelo, no mesmo {@code call_id}: as ferramentas na ordem de execução
     */
    public ToolOutputCommand answer(
            TurnRun run,
            ToolSearchCallData search
    ) {
        return this.modelCommands.toToolSearchOutput(
                search.callId(),
                this.findToolsRAGDAG(run, search.goal())
        );
    }

    /**
     * Busca as ferramentas para um objetivo e avisa a interface ({@code tool.search} e, se for o
     * caso, {@code context.suggested}).
     */
    public List<Tool> findToolsRAGDAG(
            TurnRun run,
            String goal
    ) {
        Turn turn = run.turn();
        ToolSelection selection = this.selectTools.selectRAGDAG(this.agentCommands.toSelectTools(goal, turn));

        this.hub.publish(turn.conversationId(), this.events.toolSearch(turn.id(), goal, selection));

        if (!selection.blocked().isEmpty()) {
            this.hub.publish(turn.conversationId(), this.events.contextSuggested(turn.id(), selection.blocked()));
        }

        ToolCatalog tools = this.catalog.current();

        return selection.tools().stream().map(SelectedTool::name).map(tools::require).toList();
    }
}
