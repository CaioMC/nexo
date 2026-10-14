package io.github.caiomc.nexo.adapters.conversation.google.executor;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * As chamadas REST de um contexto Google. Um executor por contexto (Gmail, Drive...).
 */
public interface GoogleContextExecutor {

    ContextKey context();

    /**
     * @throws IllegalArgumentException se a ferramenta não for deste contexto
     */
    JsonNode execute(GoogleCall call);
}
