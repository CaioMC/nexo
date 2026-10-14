package io.github.caiomc.nexo.adapters.identity.security;

/**
 * Atributos de sessão do fluxo OAuth. {@code AguardandoConsentimento} (doc 05) não é gravado no
 * banco: é {@link #PENDING_CONTEXT} durante o redirecionamento.
 */
public final class OAuthSessionAttributes {

    public static final String PENDING_CONTEXT = "nexo.pendingContext";
    public static final String PENDING_USER = "nexo.pendingUser";
    public static final String CONSENT_RETRIED = "nexo.consentRetried";

    private OAuthSessionAttributes() {
    }
}
