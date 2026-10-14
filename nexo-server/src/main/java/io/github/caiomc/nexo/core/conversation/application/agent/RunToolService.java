package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnAbortedException;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolCall;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.AgentCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.FunctionOutputMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolCallDataMapper;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;
import io.github.caiomc.nexo.core.conversation.port.tool.ToolExecutorPort;
import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolResultData;
import io.github.caiomc.nexo.core.workspace.usecase.DisableContextUseCase;

/**
 * Executa uma ferramenta já admitida (e aprovada, se for alteração) pelo {@link ToolExecutorPort}:
 * grava a chamada, emite {@code tool.started} e {@code tool.completed} e traduz o resultado para o
 * que volta ao modelo.
 */
@Service
public class RunToolService {

    private static final Logger log = LoggerFactory.getLogger(RunToolService.class);

    private final ToolExecutorPort executor;
    private final DisableContextUseCase disableContext;
    private final ToolCallRepository calls;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;

    private final ToolCallDataMapper callMapper;
    private final AgentCommandMapper agentCommands;
    private final FunctionOutputMapper functionOutputs;

    private final Clock clock;

    public RunToolService(
            ToolExecutorPort executor,
            DisableContextUseCase disableContext,
            ToolCallRepository calls,
            ConversationEventHubService hub,
            ConversationEvents events,
            ToolCallDataMapper callMapper,
            AgentCommandMapper agentCommands,
            FunctionOutputMapper functionOutputs,
            Clock clock
    ) {
        this.executor = executor;
        this.disableContext = disableContext;
        this.calls = calls;
        this.hub = hub;
        this.events = events;
        this.callMapper = callMapper;
        this.agentCommands = agentCommands;
        this.functionOutputs = functionOutputs;
        this.clock = clock;
    }

    /**
     * @return a saída da ferramenta (JSON) ou o erro explicado ao modelo
     * @throws TurnAbortedException se o Google pediu para reconectar a conta
     */
    public String run(
            TurnRun run,
            ToolCall started,
            Tool tool,
            ExecuteToolCommand command
    ) {
        Turn turn = run.turn();

        this.calls.save(this.callMapper.toData(started));
        this.hub.publish(
                turn.conversationId(),
                this.events.toolStarted(
                        turn.id(),
                        started.callId(),
                        tool.name(),
                        tool.effect().name(),
                        command.arguments()
                )
        );

        ToolResultData result = this.executeSafely(command);
        Instant now = this.clock.instant();
        ToolCall finished = result.succeeded() ? started.succeeded(result.summary(), now) : started.failed(result.error(), now);

        this.calls.save(this.callMapper.toData(finished));
        this.hub.publish(
                turn.conversationId(),
                this.events.toolCompleted(
                        turn.id(),
                        started.callId(),
                        tool.name(),
                        result.succeeded(),
                        result.summary(),
                        result.error(),
                        result.truncated()
                )
        );

        return switch (result.outcome()) {
            case SUCCEEDED -> result.output();
            case FAILED -> this.functionOutputs.toToolFailed(result.error());
            case PERMISSION_DENIED -> this.disableDeniedContext(turn, tool, result);
            case RECONNECT_REQUIRED -> throw new TurnAbortedException(FailureCode.GOOGLE_RECONNECT_REQUIRED);
        };
    }

    /**
     * Exceção do executor não derruba o turno: vira falha comum, explicada ao modelo.
     */
    private ToolResultData executeSafely(ExecuteToolCommand command) {
        try {
            return this.executor.execute(command);
        } catch (RuntimeException e) {
            log.warn("Falha executando {}: {}", command.tool(), e.getMessage());

            return this.agentCommands.toFailedResult(e.getMessage());
        }
    }

    /**
     * 403 {@code insufficientPermissions}: o escopo sumiu. O contexto é desligado (RN-01), o que
     * emite {@code context.changed}.
     */
    private String disableDeniedContext(
            Turn turn,
            Tool tool,
            ToolResultData result
    ) {
        log.warn("Google negou {} para {}: desligando o contexto {}", tool.name(), turn.userId(), tool.context());
        this.disableContext.disable(this.agentCommands.toDisableContext(turn.userId(), tool.context()));

        return this.functionOutputs.toPermissionDenied(tool, result.error());
    }
}
