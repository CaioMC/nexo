package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolCallStatus;
import io.github.caiomc.nexo.core.conversation.domain.model.Admission;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.FunctionOutputMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ModelCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolCallDataMapper;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;

/**
 * Responde a {@code function_call} do modelo aplicando a decisão do {@link ToolGatekeeperService}:
 * <ul>
 *     <li>desconhecida ou argumentos inválidos: só um erro ao modelo;</li>
 *     <li>bloqueada (contexto desligado, pré-requisito faltando): grava, emite {@code tool.blocked} e explica ao modelo;</li>
 *     <li>leitura: o {@link RunToolService} executa na hora;</li>
 *     <li>alteração: o {@link AwaitToolApprovalService} espera o usuário e só então executa.</li>
 * </ul>
 */
@Service
public class AnswerFunctionCallService {

    private final ToolGatekeeperService gatekeeper;
    private final RunToolService runTool;
    private final AwaitToolApprovalService awaitToolApproval;
    private final ToolCallRepository calls;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;

    private final ToolCallDataMapper callMapper;
    private final FunctionOutputMapper functionOutputs;
    private final ModelCommandMapper modelCommands;

    private final Clock clock;

    public AnswerFunctionCallService(
            ToolGatekeeperService gatekeeper,
            RunToolService runTool,
            AwaitToolApprovalService awaitToolApproval,
            ToolCallRepository calls,
            ConversationEventHubService hub,
            ConversationEvents events,
            ToolCallDataMapper callMapper,
            FunctionOutputMapper functionOutputs,
            ModelCommandMapper modelCommands,
            Clock clock
    ) {
        this.gatekeeper = gatekeeper;
        this.runTool = runTool;
        this.awaitToolApproval = awaitToolApproval;
        this.calls = calls;
        this.hub = hub;
        this.events = events;
        this.callMapper = callMapper;
        this.functionOutputs = functionOutputs;
        this.modelCommands = modelCommands;
        this.clock = clock;
    }

    /**
     * @return a resposta ao modelo, no mesmo {@code call_id}: a saída da ferramenta ou o motivo de não ter rodado (JSON)
     */
    public ToolOutputCommand answer(
            TurnRun run,
            FunctionCallData call
    ) {
        return this.modelCommands.toFunctionOutput(
                call.callId(),
                this.applyAdmission(run, call)
        );
    }

    private String applyAdmission(
            TurnRun run,
            FunctionCallData call
    ) {
        Admission admission = this.gatekeeper.admit(run.turn(), call);

        return switch (admission) {
            case Admission.Unknown unknown -> this.functionOutputs.toUnknownTool(unknown);
            case Admission.Invalid invalid -> this.functionOutputs.toInvalidArguments(invalid);
            case Admission.Blocked blocked -> this.block(run, call, blocked);
            case Admission.Run ready -> this.runTool.run(
                    run,
                    this.callMapper.proposed(
                            run.turn(),
                            call,
                            ready.tool(),
                            ToolCallStatus.RUNNING,
                            this.clock.instant()
                    ),
                    ready.tool(),
                    ready.command()
            );
            case Admission.AwaitApproval approval -> this.awaitToolApproval.awaitThenRun(run, call, approval);
        };
    }

    private String block(
            TurnRun run,
            FunctionCallData call,
            Admission.Blocked blocked
    ) {
        Turn turn = run.turn();

        this.calls.save(
                this.callMapper.toData(
                        this.callMapper.blocked(
                                turn,
                                call,
                                blocked.tool(),
                                blocked.reason(),
                                blocked.missing(),
                                this.clock.instant()
                        )
                )
        );
        this.hub.publish(
                turn.conversationId(),
                this.events.toolBlocked(
                        turn.id(),
                        call.callId(),
                        blocked.tool().name(),
                        blocked.reason(),
                        blocked.missing()
                )
        );

        return this.functionOutputs.toBlocked(blocked);
    }
}
