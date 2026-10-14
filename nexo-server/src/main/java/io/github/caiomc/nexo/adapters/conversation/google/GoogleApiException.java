package io.github.caiomc.nexo.adapters.conversation.google;

/**
 * Resposta de erro de uma API do Google.
 *
 * @see #insufficientPermissions()
 */
public class GoogleApiException extends RuntimeException {

    private final int status;
    private final String reason;

    public GoogleApiException(
            int status,
            String reason,
            String message
    ) {
        super(message);
        this.status = status;
        this.reason = reason;
    }

    public int status() {
        return this.status;
    }

    public String reason() {
        return this.reason;
    }

    /**
     * 403 por escopo que não foi concedido (ou sumiu).
     */
    public boolean insufficientPermissions() {
        return this.status == 403 && ("insufficientPermissions".equals(this.reason) || "ACCESS_TOKEN_SCOPE_INSUFFICIENT".equals(this.reason));
    }
}
