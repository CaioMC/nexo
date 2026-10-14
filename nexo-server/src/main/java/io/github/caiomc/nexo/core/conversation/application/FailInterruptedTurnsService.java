package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.TurnDataMapper;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;
import io.github.caiomc.nexo.core.conversation.usecase.FailInterruptedTurnsUseCase;

/**
 * RNF-08: o laço vive na memória do processo. Na subida, aprovações pendentes vencem e turnos que
 * estavam ativos viram {@code turn.failed SERVER_RESTARTED}.
 */
@Service
public class FailInterruptedTurnsService implements FailInterruptedTurnsUseCase {

    private static final Logger log = LoggerFactory.getLogger(FailInterruptedTurnsService.class);

    private final TurnRepository turns;
    private final ApprovalRepository approvals;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;
    private final TurnDataMapper mapper;
    private final Clock clock;

    public FailInterruptedTurnsService(
            TurnRepository turns,
            ApprovalRepository approvals,
            ConversationEventHubService hub,
            ConversationEvents events,
            TurnDataMapper mapper,
            Clock clock
    ) {
        this.turns = turns;
        this.approvals = approvals;
        this.hub = hub;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public int recover() {
        Instant now = this.clock.instant();

        for (ToolApprovalData approval : this.approvals.findAllPending()) {
            if (this.approvals.resolvePending(approval.id(), ApprovalStatus.EXPIRED.name(), now)) {
                this.hub.publish(approval.conversationId(), this.events.approvalResolved(approval.turnId(), approval.id(), ApprovalStatus.EXPIRED));
            }
        }

        int failed = 0;

        for (Turn turn : this.turns.findAllActive().stream().map(this.mapper::toTurn).toList()) {
            this.turns.update(this.mapper.toData(turn.failed(FailureCode.SERVER_RESTARTED, now)));
            this.hub.publish(turn.conversationId(), this.events.turnFailed(turn.id(), FailureCode.SERVER_RESTARTED));
            failed++;
        }

        if (failed > 0) {
            log.warn("{} turnos estavam ativos quando o servidor parou: encerrados com SERVER_RESTARTED", failed);
        }

        return failed;
    }
}
