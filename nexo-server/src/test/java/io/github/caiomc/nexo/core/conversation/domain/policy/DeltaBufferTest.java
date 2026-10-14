package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeltaBufferTest {

    @Test
    void batchesPiecesUntilTheWindowClosesAndKeepsTheWholeText() {
        SteppingClock clock = new SteppingClock();
        DeltaBuffer buffer = new DeltaBuffer(clock, Duration.ofMillis(50));

        assertThat(buffer.add("Pron")).isEmpty();
        clock.advance(20);
        assertThat(buffer.add("to, ")).isEmpty();
        clock.advance(40);
        assertThat(buffer.add("reunião")).contains("Pronto, reunião");
        assertThat(buffer.add(" criada.")).isEmpty();
        assertThat(buffer.drain()).contains(" criada.");
        assertThat(buffer.drain()).isEmpty();
        assertThat(buffer.text()).isEqualTo("Pronto, reunião criada.");
    }

    private static final class SteppingClock extends Clock {

        private Instant now = Instant.parse("2026-10-13T12:00:00Z");

        void advance(long millis) {
            this.now = this.now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
