package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.agent.TurnConductorService;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.NoActiveTurnException;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.TurnDataMapper;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;
import io.github.caiomc.nexo.core.conversation.usecase.StopTurnUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.StopTurnCommand;

/**
 * {@code turn.stop} (RF-23): aprovações pendentes viram {@code REJECTED} e o turno para. Se o turno
 * não estiver rodando neste processo, encerra direto no banco.
 */
@Service
public class StopTurnService implements StopTurnUseCase {

    private static final Logger log = LoggerFactory.getLogger(StopTurnService.class);

    private final ConversationQueryService queries;
    private final TurnRepository turns;
    private final ApprovalRepository approvals;
    private final ConversationEventHubService hub;
    private final TurnConductorService conductor;

    private final ConversationEvents events;
    private final TurnDataMapper mapper;
    private final Clock clock;

    public StopTurnService(
            ConversationQueryService queries,
            TurnRepository turns,
            ApprovalRepository approvals,
            ConversationEventHubService hub,
            TurnConductorService conductor,
            ConversationEvents events,
            TurnDataMapper mapper,
            Clock clock
    ) {
        this.queries = queries;
        this.turns = turns;
        this.approvals = approvals;
        this.hub = hub;
        this.conductor = conductor;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void stop(StopTurnCommand command) {
        Conversation conversation = this.queries.owned(command.userId(), command.conversationId());
        Turn turn = this.turns.findActive(conversation.id())
                .map(this.mapper::toTurn)
                .orElseThrow(() -> new NoActiveTurnException(conversation.id()));
        Instant now = this.clock.instant();

        for (ToolApprovalData pending : this.approvals.findPendingByTurn(turn.id())) {
            if (this.approvals.resolvePending(pending.id(), ApprovalStatus.REJECTED.name(), now)) {
                this.hub.publish(conversation.id(), this.events.approvalResolved(turn.id(), pending.id(), ApprovalStatus.REJECTED));
            }
        }

        if (this.conductor.stop(turn.id())) {
            log.info("Turno {} parado pelo usuário", turn.id());

            return;
        }

        this.turns.update(this.mapper.toData(turn.failed(FailureCode.STOPPED, now)));
        this.hub.publish(conversation.id(), this.events.turnFailed(turn.id(), FailureCode.STOPPED));
    }
}
