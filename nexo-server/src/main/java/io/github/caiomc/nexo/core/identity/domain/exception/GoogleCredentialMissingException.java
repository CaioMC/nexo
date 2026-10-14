package io.github.caiomc.nexo.core.identity.domain.exception;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * Não há refresh token guardado para o usuário (desconectou a conta ou nunca consentiu acesso
 * offline).
 */
public class GoogleCredentialMissingException extends RuntimeException {

    public GoogleCredentialMissingException(UserId userId) {
        super("Sem credencial Google para o usuário " + userId + ": entre de novo com o Google");
    }
}
