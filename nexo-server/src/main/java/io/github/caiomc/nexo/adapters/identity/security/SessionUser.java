package io.github.caiomc.nexo.adapters.identity.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * O que a sessão guarda do usuário logado: só o id e o e-mail. Nenhum token do Google.
 * {@link Serializable} porque a sessão vai para o banco (Spring Session JDBC).
 */
public record SessionUser(
        UUID userId,
        String email
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
