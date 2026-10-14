package io.github.caiomc.nexo.core.conversation.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.enums.TurnStatus;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurnTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final Turn turn = Turn.start(
            UUID.randomUUID(),
            UserId.of(UUID.randomUUID()),
            UUID.randomUUID(),
            "o que tenho amanhã?",
            "gpt-5.4-mini",
            List.of(ContextKey.of("calendar")),
            NOW
    );

    @Test
    void runningWaitingAndCompleted() {
        Turn waiting = this.turn.waitingApproval();
        Turn done = waiting.running().withUsage(Usage.none().plus(10, 2, 5)).completed("Nada amanhã.", NOW.plusSeconds(3));

        assertThat(waiting.status()).isEqualTo(TurnStatus.WAITING_APPROVAL);
        assertThat(done.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(done.usage().modelCalls()).isEqualTo(1);
        assertThat(done.assistantMessage()).isEqualTo("Nada amanhã.");
        assertThat(done.isActive()).isFalse();
    }

    @Test
    void finishedTurnCannotFailOrComplete() {
        Turn failed = this.turn.failed(FailureCode.STOPPED, NOW);

        assertThat(failed.failure()).isEqualTo(FailureCode.STOPPED);
        assertThatThrownBy(() -> failed.completed("x", NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void messageMustFitTheLimit() {
        assertThatThrownBy(() -> Turn.start(UUID.randomUUID(), UserId.of(UUID.randomUUID()), UUID.randomUUID(), " ", "m", List.of(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Conversation.titleFrom("  marque   reunião\ncom a Ana  ")).isEqualTo("marque reunião com a Ana");
    }
}
