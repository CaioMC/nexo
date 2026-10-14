package io.github.caiomc.nexo.core.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Usuário do Nexo. A chave natural é o {@code sub} do Google ({@code googleSubject}), nunca o
 * e-mail (RF-02).
 */
public record User(
        UserId id,
        String googleSubject,
        String email,
        String displayName,
        String pictureUrl,
        Instant createdAt,
        Instant lastLoginAt
) {

    public User {
        if (isNull(id) || isNull(googleSubject) || googleSubject.isBlank()) {
            throw new IllegalArgumentException("Usuário precisa de id e do sub do Google");
        }

        if (isNull(email) || email.isBlank()) {
            throw new IllegalArgumentException("Usuário " + googleSubject + " sem e-mail");
        }

        displayName = isNull(displayName) ? "" : displayName;
    }

    /**
     * Primeiro login: o usuário nasce com todos os contextos desligados (nenhuma linha em
     * {@code user_context}).
     */
    public static User firstLogin(
            String googleSubject,
            String email,
            String displayName,
            String pictureUrl,
            Instant now
    ) {
        return new User(
                UserId.of(UUID.randomUUID()),
                googleSubject,
                email,
                displayName,
                pictureUrl,
                now,
                now
        );
    }

    /**
     * Login de quem já existe: o Google é a fonte do e-mail, do nome e da foto.
     */
    public User loggedIn(
            String email,
            String displayName,
            String pictureUrl,
            Instant now
    ) {
        return new User(
                this.id,
                this.googleSubject,
                email,
                displayName,
                pictureUrl,
                this.createdAt,
                now
        );
    }
}
