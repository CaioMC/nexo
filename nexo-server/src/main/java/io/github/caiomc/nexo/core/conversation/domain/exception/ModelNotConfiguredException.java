package io.github.caiomc.nexo.core.conversation.domain.exception;

/**
 * Sem chave do provedor de LLM: não dá para abrir turno.
 */
public class ModelNotConfiguredException extends RuntimeException {

    public ModelNotConfiguredException() {
        super("O modelo de linguagem não está configurado: defina OPENAI_API_KEY no .env");
    }
}
