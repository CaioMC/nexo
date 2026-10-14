package io.github.caiomc.nexo.adapters.workspace.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @param status           {@code enabled} ou {@code consent_required}
 * @param authorizationUrl presente só quando é preciso ir ao Google
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContextActivationResponse(
        String status,
        String context,
        String authorizationUrl
) {

    public static final String ENABLED = "enabled";
    public static final String CONSENT_REQUIRED = "consent_required";
}
