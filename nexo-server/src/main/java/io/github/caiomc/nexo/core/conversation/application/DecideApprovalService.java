package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.agent.TurnConductorService;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.exception.ApprovalExpiredException;
import io.github.caiomc.nexo.core.conversation.domain.exception.ApprovalNotFoundException;
import io.github.caiomc.nexo.core.conversation.domain.exception.ApprovalNotPendingException;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.ToolApprovalDataMapper;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;
import io.github.caiomc.nexo.core.conversation.usecase.DecideApprovalUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.DecideApprovalCommand;

/**
 * {@code approval.decide}: valida, troca o estado de forma atômica ({@code PENDING} → decisão) e
 * acorda o turno que está esperando.
 */
@Service
public class DecideApprovalService implements DecideApprovalUseCase {

    private static final Logger log = LoggerFactory.getLogger(DecideApprovalService.class);

    private final ApprovalRepository approvals;
    private final ConversationQueryService queries;
    private final ConversationEventHubService hub;
    private final TurnConductorService conductor;

    private final ConversationEvents events;
    private final ToolApprovalDataMapper mapper;
    private final Clock clock;

    public DecideApprovalService(
            ApprovalRepository approvals,
            ConversationQueryService queries,
            ConversationEventHubService hub,
            TurnConductorService conductor,
            ConversationEvents events,
            ToolApprovalDataMapper mapper,
            Clock clock
    ) {
        this.approvals = approvals;
        this.queries = queries;
        this.hub = hub;
        this.conductor = conductor;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void decide(DecideApprovalCommand command) {
        Conversation conversation = this.queries.owned(command.userId(), command.conversationId());
        ToolApprovalData data = this.approvals.findById(command.approvalId())
                .filter(found -> found.conversationId().equals(conversation.id()))
                .orElseThrow(() -> new ApprovalNotFoundException(command.approvalId()));
        ToolApproval approval = this.mapper.toApproval(data);
        Instant now = this.clock.instant();

        if (approval.status() == ApprovalStatus.EXPIRED) {
            throw new ApprovalExpiredException(approval.id());
        }

        if (approval.status() != ApprovalStatus.PENDING) {
            throw new ApprovalNotPendingException(approval.id(), approval.status());
        }

        if (approval.expiredAt(now)) {
            this.resolve(
                    conversation,
                    approval,
                    ApprovalStatus.EXPIRED,
                    now
            );

            throw new ApprovalExpiredException(approval.id());
        }

        boolean decided = this.resolve(
                conversation,
                approval,
                command.decision().status(),
                now
        );

        if (!decided) {
            throw new ApprovalNotPendingException(approval.id(), command.decision().status());
        }

        log.info("Aprovação {} ({}): {}", approval.id(), approval.tool(), command.decision());
    }

    private boolean resolve(
            Conversation conversation,
            ToolApproval approval,
            ApprovalStatus status,
            Instant now
    ) {
        if (!this.approvals.resolvePending(approval.id(), status.name(), now)) {
            return false;
        }

        this.hub.publish(conversation.id(), this.events.approvalResolved(approval.turnId(), approval.id(), status));
        this.conductor.deliverDecision(approval.turnId(), approval.id(), status);

        return true;
    }
}
