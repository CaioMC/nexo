package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnAbortedException;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.model.ModelSession;

import static java.util.Objects.nonNull;

/**
 * Um turno rodando agora, numa virtual thread: o estado atual, o botão Parar, as aprovações
 * esperadas e o tempo rodando (esperar aprovação não conta para o limite, RN-11).
 *
 * <p>Esperar uma aprovação bloqueia a virtual thread do turno, o que é barato; a decisão chega por
 * {@link #deliverDecision} vinda de outra thread ({@code DecideApprovalService}, {@code ExpireApprovalsService}).
 */
public final class TurnRun {

    private final ZoneId zone;
    private final Map<UUID, CompletableFuture<ApprovalStatus>> decisions = new ConcurrentHashMap<>();

    private volatile Turn turn;
    private volatile ModelSession session;
    private volatile boolean stopped;

    private long runningNanos;
    private long resumedAt;

    private TurnRun(
            Turn turn,
            ZoneId zone
    ) {
        this.turn = turn;
        this.zone = zone;
        this.resumedAt = System.nanoTime();
    }

    public static TurnRun of(
            Turn turn,
            ZoneId zone
    ) {
        return new TurnRun(turn, zone);
    }

    public Turn turn() {
        return this.turn;
    }

    public ZoneId zone() {
        return this.zone;
    }

    /**
     * Guarda o novo estado do turno (só em memória; quem grava é o {@link RecordTurnStateService}).
     */
    public void updateTurn(Turn current) {
        this.turn = current;
    }

    /**
     * A sessão aberta com o modelo, para o botão Parar conseguir cancelar a chamada em andamento.
     */
    public void attachModelSession(ModelSession current) {
        this.session = current;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * Botão Parar: cancela a chamada ao modelo e acorda quem espera aprovação.
     */
    public void stop() {
        this.stopped = true;

        ModelSession current = this.session;

        if (nonNull(current)) {
            current.cancel();
        }

        this.decisions.values().forEach(decision -> decision.complete(ApprovalStatus.REJECTED));
    }

    /**
     * Para o laço se o usuário mandou parar ou o turno passou do tempo rodando.
     */
    public void ensureCanContinue(Duration timeout) {
        if (this.stopped) {
            throw new TurnAbortedException(FailureCode.STOPPED);
        }

        if (this.runningTime().compareTo(timeout) > 0) {
            throw new TurnAbortedException(FailureCode.TIMEOUT);
        }
    }

    /**
     * Tempo rodando, sem contar o tempo esperando aprovação.
     */
    public Duration runningTime() {
        return Duration.ofNanos(this.runningNanos + System.nanoTime() - this.resumedAt);
    }

    /**
     * Espera a decisão do usuário. O tempo esperando não conta para o limite do turno.
     *
     * @return {@code EXPIRED} se ninguém decidiu dentro de {@code ttl}
     */
    public ApprovalStatus awaitDecision(
            UUID approvalId,
            Duration ttl
    ) {
        CompletableFuture<ApprovalStatus> decision = this.decisions.computeIfAbsent(approvalId, key -> new CompletableFuture<>());

        if (this.stopped) {
            decision.complete(ApprovalStatus.REJECTED);
        }

        this.runningNanos += System.nanoTime() - this.resumedAt;

        try {
            return decision.get(Math.max(1, ttl.toMillis()), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return ApprovalStatus.EXPIRED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TurnAbortedException(FailureCode.STOPPED);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha esperando a aprovação " + approvalId, e);
        } finally {
            this.decisions.remove(approvalId);
            this.resumedAt = System.nanoTime();
        }
    }

    /**
     * A decisão chegou (ou a aprovação venceu).
     */
    public void deliverDecision(
            UUID approvalId,
            ApprovalStatus status
    ) {
        this.decisions.computeIfAbsent(approvalId, key -> new CompletableFuture<>()).complete(status);
    }
}
