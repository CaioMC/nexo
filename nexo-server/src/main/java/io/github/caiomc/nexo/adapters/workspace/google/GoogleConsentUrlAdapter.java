package io.github.caiomc.nexo.adapters.workspace.google;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.workspace.port.consent.ConsentUrlPort;
import io.github.caiomc.nexo.core.workspace.port.consent.command.ConsentUrlCommand;

/**
 * O consentimento começa no próprio servidor: o {@code IncrementalAuthorizationRequestResolver}
 * lê {@code ?context=} e monta o pedido ao Google com os escopos do contexto.
 */
@Component
public class GoogleConsentUrlAdapter implements ConsentUrlPort {

    @Override
    public String consentUrl(ConsentUrlCommand command) {
        return "/oauth2/authorization/google?context=" + command.context().value();
    }
}
