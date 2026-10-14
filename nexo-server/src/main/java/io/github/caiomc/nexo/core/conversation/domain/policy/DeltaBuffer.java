package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Junta os pedaços de texto do streaming em lotes de até 50 ms, para não gravar um evento por
 * token (doc 06, seção 4). Usado por uma thread só (a do turno).
 */
public class DeltaBuffer {

    private final Clock clock;
    private final Duration window;
    private final StringBuilder pending = new StringBuilder();
    private final StringBuilder all = new StringBuilder();

    private Instant first;

    public DeltaBuffer(
            Clock clock,
            Duration window
    ) {
        this.clock = clock;
        this.window = window;
    }

    /**
     * @return o lote pronto para virar {@code assistant.delta}, se a janela fechou
     */
    public Optional<String> add(String text) {
        Instant now = this.clock.instant();

        if (isNull(this.first)) {
            this.first = now;
        }

        this.pending.append(text);
        this.all.append(text);

        return Duration.between(this.first, now).compareTo(this.window) >= 0 ? this.drain() : Optional.empty();
    }

    /**
     * O que sobrou no buffer (fim da resposta).
     */
    public Optional<String> drain() {
        if (this.pending.isEmpty()) {
            return Optional.empty();
        }

        String batch = this.pending.toString();

        this.pending.setLength(0);
        this.first = null;

        return Optional.of(batch);
    }

    /**
     * Todo o texto recebido desde o começo.
     */
    public String text() {
        return this.all.toString();
    }
}
