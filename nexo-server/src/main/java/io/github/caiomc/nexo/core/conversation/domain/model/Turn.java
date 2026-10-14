package io.github.caiomc.nexo.core.conversation.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.enums.TurnStatus;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Uma mensagem do usuário e todo o trabalho do assistente até a resposta (doc 11).
 *
 * @param enabledContexts foto dos contextos ligados no começo (auditoria); o gatekeeper relê na hora
 */
public record Turn(
        UUID id,
        UUID conversationId,
        UserId userId,
        UUID clientMessageId,
        String userMessage,
        String assistantMessage,
        TurnStatus status,
        FailureCode failure,
        String model,
        List<ContextKey> enabledContexts,
        Usage usage,
        Instant startedAt,
        Instant finishedAt
) {

    public static final int MAX_MESSAGE = 8000;

    public Turn {
        if (isNull(id) || isNull(conversationId) || isNull(userId) || isNull(clientMessageId)) {
            throw new IllegalArgumentException("Turno precisa de id, conversa, usuário e clientMessageId");
        }

        if (isNull(userMessage) || userMessage.isBlank() || userMessage.length() > MAX_MESSAGE) {
            throw new IllegalArgumentException("A mensagem precisa ter de 1 a " + MAX_MESSAGE + " caracteres");
        }

        enabledContexts = isNull(enabledContexts) ? List.of() : List.copyOf(enabledContexts);
        usage = isNull(usage) ? Usage.none() : usage;
    }

    public static Turn start(
            UUID conversationId,
            UserId userId,
            UUID clientMessageId,
            String userMessage,
            String model,
            List<ContextKey> enabledContexts,
            Instant now
    ) {
        return new Turn(
                UUID.randomUUID(),
                conversationId,
                userId,
                clientMessageId,
                userMessage,
                null,
                TurnStatus.RUNNING,
                null,
                model,
                enabledContexts,
                Usage.none(),
                now,
                null
        );
    }

    public boolean isActive() {
        return this.status.isActive();
    }

    public Turn waitingApproval() {
        this.requireActive();

        return this.with(TurnStatus.WAITING_APPROVAL, this.usage);
    }

    public Turn running() {
        this.requireActive();

        return this.with(TurnStatus.RUNNING, this.usage);
    }

    public Turn withUsage(Usage current) {
        return this.with(this.status, current);
    }

    public Turn completed(
            String text,
            Instant now
    ) {
        this.requireActive();

        return new Turn(
                this.id,
                this.conversationId,
                this.userId,
                this.clientMessageId,
                this.userMessage,
                text,
                TurnStatus.COMPLETED,
                null,
                this.model,
                this.enabledContexts,
                this.usage,
                this.startedAt,
                now
        );
    }

    public Turn failed(
            FailureCode code,
            Instant now
    ) {
        this.requireActive();

        return new Turn(
                this.id,
                this.conversationId,
                this.userId,
                this.clientMessageId,
                this.userMessage,
                null,
                TurnStatus.FAILED,
                code,
                this.model,
                this.enabledContexts,
                this.usage,
                this.startedAt,
                now
        );
    }

    private Turn with(
            TurnStatus next,
            Usage current
    ) {
        return new Turn(
                this.id,
                this.conversationId,
                this.userId,
                this.clientMessageId,
                this.userMessage,
                this.assistantMessage,
                next,
                this.failure,
                this.model,
                this.enabledContexts,
                current,
                this.startedAt,
                this.finishedAt
        );
    }

    private void requireActive() {
        if (!this.isActive()) {
            throw new IllegalStateException("O turno " + this.id + " já terminou (" + this.status + ")");
        }
    }
}
