package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.domain.model.Admission;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsHasher;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsValidator;
import io.github.caiomc.nexo.core.conversation.mapper.AgentCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ExecuteToolCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ExecutedToolMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolApprovalDataMapper;
import io.github.caiomc.nexo.core.conversation.port.approval.ApprovalDescriberPort;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;
import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * O porteiro (doc 11, seção 4): o modelo propõe, o gatekeeper decide se a chamada roda. Na ordem:
 * existe? contexto ligado agora? argumentos válidos? pré-requisitos rodaram? leitura ou alteração?
 *
 * <p>Só decide, sem efeito colateral: nada é gravado nem emitido aqui. Quem aplica a decisão é o
 * {@link AnswerFunctionCallService}.
 */
@Service
public class ToolGatekeeperService {

    private final ProvideCatalogUseCase catalog;
    private final GetEnabledContextsUseCase contexts;
    private final ToolCallRepository calls;
    private final ApprovalDescriberPort describer;

    private final ArgumentsValidator validator;
    private final ArgumentsHasher hasher;

    private final ExecuteToolCommandMapper executeMapper;
    private final ExecutedToolMapper executedMapper;
    private final ToolApprovalDataMapper approvalMapper;
    private final AgentCommandMapper agentCommands;

    private final AgentSettings settings;
    private final Clock clock;

    public ToolGatekeeperService(
            ProvideCatalogUseCase catalog,
            GetEnabledContextsUseCase contexts,
            ToolCallRepository calls,
            ApprovalDescriberPort describer,
            ArgumentsValidator validator,
            ArgumentsHasher hasher,
            ExecuteToolCommandMapper executeMapper,
            ExecutedToolMapper executedMapper,
            ToolApprovalDataMapper approvalMapper,
            AgentCommandMapper agentCommands,
            AgentSettings settings,
            Clock clock
    ) {
        this.catalog = catalog;
        this.contexts = contexts;
        this.calls = calls;
        this.describer = describer;
        this.validator = validator;
        this.hasher = hasher;
        this.executeMapper = executeMapper;
        this.executedMapper = executedMapper;
        this.approvalMapper = approvalMapper;
        this.agentCommands = agentCommands;
        this.settings = settings;
        this.clock = clock;
    }

    public Admission admit(
            Turn turn,
            FunctionCallData call
    ) {
        ToolCatalog tools = this.catalog.current();
        Tool tool = this.findTool(tools, call.name()).orElse(null);

        if (isNull(tool)) {
            return Admission.unknown(call.name());
        }

        Set<ContextKey> enabled = this.contexts.enabled(this.agentCommands.toEnabledContexts(turn.userId()));

        if (!enabled.contains(tool.context())) {
            return Admission.blocked(tool, BlockReason.CONTEXT_DISABLED, List.of(tool.context().value()));
        }

        List<String> errors = nonNull(call.argumentsError()) ? List.of(call.argumentsError()) : this.validator.validate(tool, call.arguments());

        if (!errors.isEmpty()) {
            return Admission.invalid(tool, errors);
        }

        List<ToolName> missing = this.missingPrerequisitesDAG(tools, tool, turn);

        if (!missing.isEmpty()) {
            return Admission.blocked(tool, BlockReason.MISSING_PREREQUISITES, ToolName.values(missing));
        }

        ExecuteToolCommand command = this.executeMapper.toCommand(turn, tool, call);

        return tool.effect().requiresApproval() ?
                Admission.awaitApproval(tool, this.pendingApproval(turn, tool, call), command) :
                Admission.run(tool, command);
    }

    /**
     * Pré-requisitos diretos no DAG que não rodaram com sucesso na conversa dentro da janela (RN-04).
     */
    private List<ToolName> missingPrerequisitesDAG(
            ToolCatalog tools,
            Tool tool,
            Turn turn
    ) {
        return tools.graph().missingPrerequisitesDAG(tool.name(), this.recentlyExecuted(turn));
    }

    /**
     * Ferramentas que rodaram com sucesso na conversa dentro da janela de pré-requisitos (RN-04).
     */
    private Set<ToolName> recentlyExecuted(Turn turn) {
        return this.executedMapper.toNames(
                this.calls.succeededSince(
                        turn.conversationId(),
                        this.clock.instant().minus(this.settings.prerequisiteWindow())
                )
        );
    }

    private ToolApproval pendingApproval(
            Turn turn,
            Tool tool,
            FunctionCallData call
    ) {
        return this.approvalMapper.pending(
                turn,
                call,
                tool,
                this.describer.describe(this.agentCommands.toDescribeApproval(tool, call.arguments())),
                this.hasher.hash(call.arguments()),
                this.clock.instant(),
                this.settings.approvalTtl()
        );
    }

    private Optional<Tool> findTool(
            ToolCatalog tools,
            String name
    ) {
        try {
            return tools.find(ToolName.of(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
