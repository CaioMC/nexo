package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnAbortedException;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurnRunTest {

    private final TurnRun run = TurnRun.of(
            Turn.start(UUID.randomUUID(), UserId.of(UUID.randomUUID()), UUID.randomUUID(), "oi", "m", List.of(), Instant.now()),
            ZoneOffset.UTC
    );

    @Test
    void decisionThatArrivesBeforeTheWaitIsNotLost() {
        UUID approval = UUID.randomUUID();

        this.run.deliverDecision(approval, ApprovalStatus.APPROVED);

        assertThat(this.run.awaitDecision(approval, Duration.ofSeconds(1))).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void nobodyDecidingInTimeIsExpired() {
        assertThat(this.run.awaitDecision(UUID.randomUUID(), Duration.ofMillis(20))).isEqualTo(ApprovalStatus.EXPIRED);
    }

    @Test
    void stopWakesTheWaitingThreadWithRejectedAndAbortsTheLoop() throws Exception {
        CompletableFuture<ApprovalStatus> waiting = CompletableFuture.supplyAsync(() -> this.run.awaitDecision(UUID.randomUUID(), Duration.ofSeconds(5)));

        Thread.sleep(50);
        this.run.stop();

        assertThat(waiting.get()).isEqualTo(ApprovalStatus.REJECTED);
        assertThatThrownBy(() -> this.run.ensureCanContinue(Duration.ofMinutes(3)))
                .isInstanceOf(TurnAbortedException.class)
                .extracting(e -> ((TurnAbortedException) e).code())
                .isEqualTo(FailureCode.STOPPED);
    }

    @Test
    void timeWaitingForApprovalDoesNotCountAgainstTheTimeout() {
        this.run.awaitDecision(UUID.randomUUID(), Duration.ofMillis(120));

        assertThat(this.run.runningTime()).isLessThan(Duration.ofMillis(100));
        assertThatThrownBy(() -> this.run.ensureCanContinue(Duration.ZERO))
                .isInstanceOf(TurnAbortedException.class)
                .extracting(e -> ((TurnAbortedException) e).code())
                .isEqualTo(FailureCode.TIMEOUT);
    }
}
