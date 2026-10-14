package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnAbortedException;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;

/**
 * O orquestrador (doc 11, seção 1): cada turno roda o {@link AgentLoopService} numa virtual thread. Aqui
 * o turno começa, recebe Parar e decisões de aprovação de outras threads, e vira
 * {@code turn.failed} quando algo dá errado (limite, timeout, Parar, erro do modelo).
 */
@Service
public class TurnConductorService {

    private static final Logger log = LoggerFactory.getLogger(TurnConductorService.class);

    private final AgentLoopService agentLoop;
    private final RecordTurnStateService recordTurnState;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;
    private final Clock clock;

    private final Map<UUID, TurnRun> runs = new ConcurrentHashMap<>();

    public TurnConductorService(
            AgentLoopService agentLoop,
            RecordTurnStateService recordTurnState,
            ConversationEventHubService hub,
            ConversationEvents events,
            Clock clock
    ) {
        this.agentLoop = agentLoop;
        this.recordTurnState = recordTurnState;
        this.hub = hub;
        this.events = events;
        this.clock = clock;
    }

    public void start(
            Turn turn,
            ZoneId zone
    ) {
        TurnRun run = TurnRun.of(turn, zone);

        this.runs.put(turn.id(), run);
        this.hub.publish(turn.conversationId(), this.events.turnStarted(turn.id(), turn.model(), turn.enabledContexts()));
        Thread.ofVirtual().name("turn-" + turn.id()).start(() -> this.runAgentLoop(run));
    }

    /**
     * @return {@code false} se o turno não está rodando neste processo
     */
    public boolean stop(UUID turnId) {
        Optional<TurnRun> run = Optional.ofNullable(this.runs.get(turnId));

        run.ifPresent(TurnRun::stop);

        return run.isPresent();
    }

    /**
     * Entrega a decisão (ou o vencimento) da aprovação ao turno que está esperando.
     */
    public void deliverDecision(
            UUID turnId,
            UUID approvalId,
            ApprovalStatus status
    ) {
        Optional.ofNullable(this.runs.get(turnId)).ifPresent(run -> run.deliverDecision(approvalId, status));
    }

    public boolean isRunning(UUID turnId) {
        return this.runs.containsKey(turnId);
    }

    private void runAgentLoop(TurnRun run) {
        try {
            this.agentLoop.runTurn(run);
        } catch (TurnAbortedException e) {
            this.failTurn(run, e.code());
        } catch (RuntimeException e) {
            log.warn("Turno {} falhou: {}", run.turn().id(), e.getMessage(), e);
            this.failTurn(run, run.isStopped() ? FailureCode.STOPPED : FailureCode.MODEL_ERROR);
        } finally {
            this.runs.remove(run.turn().id());
        }
    }

    private void failTurn(
            TurnRun run,
            FailureCode code
    ) {
        Turn turn = run.turn();

        if (!turn.isActive()) {
            return;
        }

        this.recordTurnState.record(run, turn.failed(code, this.clock.instant()));
        this.hub.publish(turn.conversationId(), this.events.turnFailed(turn.id(), code));
        log.info("Turno {} terminou com {}", turn.id(), code);
    }
}
