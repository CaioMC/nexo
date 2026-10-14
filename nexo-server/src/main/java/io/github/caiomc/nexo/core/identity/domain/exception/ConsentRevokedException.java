package io.github.caiomc.nexo.core.identity.domain.exception;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * O Google recusou o refresh token ({@code invalid_grant}): o usuário revogou o acesso ou o token
 * venceu. É preciso entrar de novo.
 */
public class ConsentRevokedException extends RuntimeException {

    public ConsentRevokedException(UserId userId) {
        super("O acesso ao Google do usuário " + userId + " foi revogado ou venceu: reconecte a conta");
    }
}
