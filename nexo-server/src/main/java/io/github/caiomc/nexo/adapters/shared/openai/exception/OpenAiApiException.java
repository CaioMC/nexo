package io.github.caiomc.nexo.adapters.shared.openai.exception;

/**
 * Resposta não 2xx da API. A mensagem leva o status e o corpo da resposta (que não contém a chave).
 */
public class OpenAiApiException extends RuntimeException {

    private static final int MAX_BODY = 500;

    private final int status;

    public OpenAiApiException(
            int status,
            String path,
            String body
    ) {
        super("API do modelo respondeu " + status + " em " + path + ": " + (body.length() > MAX_BODY ? body.substring(0, MAX_BODY) + "..." : body));
        this.status = status;
    }

    public int status() {
        return this.status;
    }
}
