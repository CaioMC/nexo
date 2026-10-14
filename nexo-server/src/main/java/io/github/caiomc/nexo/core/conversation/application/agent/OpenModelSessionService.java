package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.ContextSummary;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolCallStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolCall;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.InstructionsBuilder;
import io.github.caiomc.nexo.core.conversation.mapper.AgentCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ModelCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolCallDataMapper;
import io.github.caiomc.nexo.core.conversation.mapper.TurnDataMapper;
import io.github.caiomc.nexo.core.conversation.port.model.LanguageModelPort;
import io.github.caiomc.nexo.core.conversation.port.model.ModelSession;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;

/**
 * Prepara tudo o que vai no prompt antes da 1ª chamada ao modelo e abre a sessão:
 * <ol>
 *     <li>instruções do sistema, com os contextos ligados/desligados e a data no fuso do usuário;</li>
 *     <li>histórico: as mensagens finais dos últimos turnos da conversa;</li>
 *     <li>ferramentas pré-carregadas: as usadas com sucesso nos últimos turnos (memória) e, no modo
 *     {@code PRESELECTED}, as encontradas pela busca com a mensagem do usuário;</li>
 *     <li>namespaces dos contextos ligados, citados na descrição do {@code tool_search}.</li>
 * </ol>
 */
@Service
public class OpenModelSessionService {

    private final LanguageModelPort model;
    private final ProvideCatalogUseCase catalog;
    private final GetEnabledContextsUseCase contexts;
    private final AnswerToolSearchService answerToolSearch;
    private final TurnRepository turns;
    private final ToolCallRepository calls;

    private final InstructionsBuilder instructions;

    private final AgentCommandMapper agentCommands;
    private final ModelCommandMapper modelCommands;
    private final TurnDataMapper turnMapper;
    private final ToolCallDataMapper callMapper;

    private final AgentSettings settings;
    private final Clock clock;

    public OpenModelSessionService(
            LanguageModelPort model,
            ProvideCatalogUseCase catalog,
            GetEnabledContextsUseCase contexts,
            AnswerToolSearchService answerToolSearch,
            TurnRepository turns,
            ToolCallRepository calls,
            InstructionsBuilder instructions,
            AgentCommandMapper agentCommands,
            ModelCommandMapper modelCommands,
            TurnDataMapper turnMapper,
            ToolCallDataMapper callMapper,
            AgentSettings settings,
            Clock clock
    ) {
        this.model = model;
        this.catalog = catalog;
        this.contexts = contexts;
        this.answerToolSearch = answerToolSearch;
        this.turns = turns;
        this.calls = calls;
        this.instructions = instructions;
        this.agentCommands = agentCommands;
        this.modelCommands = modelCommands;
        this.turnMapper = turnMapper;
        this.callMapper = callMapper;
        this.settings = settings;
        this.clock = clock;
    }

    public ModelSession open(TurnRun run) {
        Turn turn = run.turn();
        ToolCatalog tools = this.catalog.current();
        Set<ContextKey> enabled = this.contexts.enabled(this.agentCommands.toEnabledContexts(turn.userId()));
        List<Turn> previousTurns = this.loadPreviousTurns(turn);
        Map<UUID, List<ToolCall>> callsByTurn = this.loadCallsByTurn(previousTurns);
        List<Tool> preloadedTools = this.preloadedTools(
                run,
                tools,
                enabled,
                previousTurns,
                callsByTurn
        );

        return this.model.openSession(
                this.modelCommands.toOpenSession(
                        this.instructions.build(
                                enabled,
                                this.disabledContexts(tools, enabled),
                                this.clock.instant(),
                                run.zone()
                        ),
                        this.modelCommands.toHistory(previousTurns, callsByTurn),
                        turn,
                        this.settings.exposure(),
                        preloadedTools,
                        this.enabledNamespaces(tools, enabled)
                )
        );
    }

    /**
     * Os últimos turnos terminados da conversa, do mais antigo ao mais novo.
     */
    private List<Turn> loadPreviousTurns(Turn turn) {
        List<Turn> recent = new ArrayList<>(
                this.turns.findFinished(turn.conversationId(), this.settings.historyTurns())
                        .stream()
                        .map(this.turnMapper::toTurn)
                        .toList()
        );

        recent.sort((first, second) -> first.startedAt().compareTo(second.startedAt()));

        return recent;
    }

    private Map<UUID, List<ToolCall>> loadCallsByTurn(List<Turn> previousTurns) {
        if (previousTurns.isEmpty()) {
            return Map.of();
        }

        return this.calls.findByTurns(previousTurns.stream().map(Turn::id).toList())
                .stream()
                .map(this.callMapper::toCall)
                .collect(Collectors.groupingBy(ToolCall::turnId));
    }

    private List<Tool> preloadedTools(
            TurnRun run,
            ToolCatalog tools,
            Set<ContextKey> enabled,
            List<Turn> previousTurns,
            Map<UUID, List<ToolCall>> callsByTurn
    ) {
        Set<Tool> preloaded = new LinkedHashSet<>(
                this.rememberedToolsDAG(
                        tools,
                        enabled,
                        previousTurns,
                        callsByTurn
                )
        );

        if (this.settings.exposure() == ToolExposure.PRESELECTED) {
            preloaded.addAll(this.answerToolSearch.findToolsRAGDAG(run, run.turn().userMessage()));
        }

        return List.copyOf(preloaded);
    }

    /**
     * Memória de ferramentas: as usadas com sucesso nos últimos turnos já vão carregadas, se ainda
     * estiverem disponíveis (contexto do fecho ligado).
     */
    private List<Tool> rememberedToolsDAG(
            ToolCatalog tools,
            Set<ContextKey> enabled,
            List<Turn> previousTurns,
            Map<UUID, List<ToolCall>> callsByTurn
    ) {
        List<ToolName> used = previousTurns.stream()
                .skip(Math.max(0, previousTurns.size() - this.settings.toolMemoryTurns()))
                .flatMap(turn -> callsByTurn.getOrDefault(turn.id(), List.of()).stream())
                .filter(call -> call.status() == ToolCallStatus.SUCCEEDED)
                .map(ToolCall::tool)
                .distinct()
                .sorted()
                .toList();

        return tools.availabilityDAG(used, enabled)
                .available()
                .stream()
                .map(tools::require)
                .toList();
    }

    private List<ContextKey> disabledContexts(
            ToolCatalog tools,
            Collection<ContextKey> enabled
    ) {
        return tools.overview()
                .byContext()
                .stream()
                .map(ContextSummary::context)
                .filter(context -> !enabled.contains(context))
                .toList();
    }

    private List<String> enabledNamespaces(
            ToolCatalog tools,
            Collection<ContextKey> enabled
    ) {
        return tools.overview()
                .byContext()
                .stream()
                .filter(summary -> enabled.contains(summary.context()))
                .flatMap(summary -> summary.namespaces().stream())
                .toList();
    }
}
