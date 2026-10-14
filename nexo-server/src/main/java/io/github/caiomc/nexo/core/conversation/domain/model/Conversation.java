package io.github.caiomc.nexo.core.conversation.domain.model;

import java.time.Instant;
import java.util.UUID;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Uma conversa do usuário com o assistente. {@code lastSeq} é o último evento gravado.
 */
public record Conversation(
        UUID id,
        UserId userId,
        String title,
        long lastSeq,
        Instant createdAt,
        Instant updatedAt
) {

    public static final String DEFAULT_TITLE = "Nova conversa";
    public static final int MAX_TITLE = 120;

    public Conversation {
        if (isNull(id) || isNull(userId)) {
            throw new IllegalArgumentException("Conversa precisa de id e usuário");
        }

        title = normalizeTitle(title);
    }

    public static Conversation start(
            UserId userId,
            String title,
            Instant now
    ) {
        return new Conversation(
                UUID.randomUUID(),
                userId,
                isNull(title) || title.isBlank() ? DEFAULT_TITLE : title,
                0,
                now,
                now
        );
    }

    /**
     * Título automático do 1º turno: o começo da mensagem do usuário.
     */
    public static String titleFrom(String message) {
        String line = message.strip().replaceAll("\\s+", " ");

        return line.length() <= 60 ? line : line.substring(0, 57).strip() + "...";
    }

    private static String normalizeTitle(String title) {
        if (isNull(title) || title.isBlank()) {
            throw new IllegalArgumentException("O título da conversa não pode ficar vazio");
        }

        String stripped = title.strip();

        if (stripped.length() > MAX_TITLE) {
            throw new IllegalArgumentException("O título tem no máximo " + MAX_TITLE + " caracteres");
        }

        return stripped;
    }

    public boolean belongsTo(UserId user) {
        return this.userId.equals(user);
    }

    public boolean hasDefaultTitle() {
        return DEFAULT_TITLE.equals(this.title);
    }

    public Conversation renamed(
            String newTitle,
            Instant now
    ) {
        return new Conversation(
                this.id,
                this.userId,
                newTitle,
                this.lastSeq,
                this.createdAt,
                now
        );
    }
}
