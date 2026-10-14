package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.agent.TurnConductorService;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;
import io.github.caiomc.nexo.core.conversation.usecase.ExpireApprovalsUseCase;

/**
 * RN-06: aprovação pendente vence em 15 minutos e conta como recusa; o turno segue com "o usuário
 * não respondeu a tempo".
 */
@Service
public class ExpireApprovalsService implements ExpireApprovalsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireApprovalsService.class);

    private final ApprovalRepository approvals;
    private final ConversationEventHubService hub;
    private final TurnConductorService conductor;

    private final ConversationEvents events;
    private final Clock clock;

    public ExpireApprovalsService(
            ApprovalRepository approvals,
            ConversationEventHubService hub,
            TurnConductorService conductor,
            ConversationEvents events,
            Clock clock
    ) {
        this.approvals = approvals;
        this.hub = hub;
        this.conductor = conductor;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public int expire() {
        Instant now = this.clock.instant();
        int expired = 0;

        for (ToolApprovalData approval : this.approvals.findPendingExpiredAt(now)) {
            if (!this.approvals.resolvePending(approval.id(), ApprovalStatus.EXPIRED.name(), now)) {
                continue;
            }

            this.hub.publish(approval.conversationId(), this.events.approvalResolved(approval.turnId(), approval.id(), ApprovalStatus.EXPIRED));
            this.conductor.deliverDecision(approval.turnId(), approval.id(), ApprovalStatus.EXPIRED);
            expired++;
        }

        if (expired > 0) {
            log.info("{} aprovações venceram", expired);
        }

        return expired;
    }
}
