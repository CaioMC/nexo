package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolCallStatus;
import io.github.caiomc.nexo.core.conversation.domain.model.Admission;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolCall;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsHasher;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.FunctionOutputMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolApprovalDataMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolCallDataMapper;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;

/**
 * Alterações pedem confirmação (RF-35): grava a chamada e a aprovação pendente, emite
 * {@code approval.requested} e pausa o turno até o usuário decidir ou a aprovação vencer.
 * Aprovada: confere o hash dos argumentos e consome na mesma operação (uso único, RN-05) antes de
 * entregar ao {@link RunToolService}.
 */
@Service
public class AwaitToolApprovalService {

    private final RunToolService runTool;
    private final RecordTurnStateService recordTurnState;
    private final ToolCallRepository calls;
    private final ApprovalRepository approvals;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;
    private final ArgumentsHasher hasher;

    private final ToolCallDataMapper callMapper;
    private final ToolApprovalDataMapper approvalMapper;
    private final FunctionOutputMapper functionOutputs;

    private final AgentSettings settings;
    private final Clock clock;

    public AwaitToolApprovalService(
            RunToolService runTool,
            RecordTurnStateService recordTurnState,
            ToolCallRepository calls,
            ApprovalRepository approvals,
            ConversationEventHubService hub,
            ConversationEvents events,
            ArgumentsHasher hasher,
            ToolCallDataMapper callMapper,
            ToolApprovalDataMapper approvalMapper,
            FunctionOutputMapper functionOutputs,
            AgentSettings settings,
            Clock clock
    ) {
        this.runTool = runTool;
        this.recordTurnState = recordTurnState;
        this.calls = calls;
        this.approvals = approvals;
        this.hub = hub;
        this.events = events;
        this.hasher = hasher;
        this.callMapper = callMapper;
        this.approvalMapper = approvalMapper;
        this.functionOutputs = functionOutputs;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * @return a saída da ferramenta, se aprovada, ou o motivo de não ter rodado
     */
    public String awaitThenRun(
            TurnRun run,
            FunctionCallData call,
            Admission.AwaitApproval admission
    ) {
        ToolApproval approval = admission.approval();
        ToolCall waiting = this.requestApproval(run, call, admission);

        ApprovalStatus decision = run.awaitDecision(approval.id(), Duration.between(this.clock.instant(), approval.expiresAt()));

        if (decision == ApprovalStatus.EXPIRED) {
            this.expire(run, approval);
        }

        run.ensureCanContinue(this.settings.turnTimeout());
        this.recordTurnState.record(run, run.turn().running());

        if (decision == ApprovalStatus.APPROVED && this.approvals.consume(approval.id(), this.hasher.hash(admission.command().arguments()))) {
            return this.runTool.run(
                    run,
                    waiting.running(),
                    admission.tool(),
                    admission.command()
            );
        }

        this.calls.save(this.callMapper.toData(waiting.rejected(this.clock.instant())));

        return decision == ApprovalStatus.EXPIRED ?
                this.functionOutputs.toApprovalExpired() :
                this.functionOutputs.toApprovalRejected();
    }

    private ToolCall requestApproval(
            TurnRun run,
            FunctionCallData call,
            Admission.AwaitApproval admission
    ) {
        ToolApproval approval = admission.approval();
        ToolCall waiting = this.callMapper.proposed(
                run.turn(),
                call,
                admission.tool(),
                ToolCallStatus.WAITING_APPROVAL,
                this.clock.instant()
        );

        this.calls.save(this.callMapper.toData(waiting));
        this.approvals.insert(this.approvalMapper.toData(approval, run.turn()));
        this.recordTurnState.record(run, run.turn().waitingApproval());
        this.hub.publish(run.turn().conversationId(), this.events.approvalRequested(approval));

        return waiting;
    }

    private void expire(
            TurnRun run,
            ToolApproval approval
    ) {
        if (this.approvals.resolvePending(approval.id(), ApprovalStatus.EXPIRED.name(), this.clock.instant())) {
            this.hub.publish(run.turn().conversationId(), this.events.approvalResolved(approval.turnId(), approval.id(), ApprovalStatus.EXPIRED));
        }
    }
}
