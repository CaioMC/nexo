package io.github.caiomc.nexo.core.workspace.port.consent;

import io.github.caiomc.nexo.core.workspace.port.consent.command.ConsentUrlCommand;

/**
 * Para onde o navegador vai para conceder os escopos de um contexto.
 */
public interface ConsentUrlPort {

    String consentUrl(ConsentUrlCommand command);
}
