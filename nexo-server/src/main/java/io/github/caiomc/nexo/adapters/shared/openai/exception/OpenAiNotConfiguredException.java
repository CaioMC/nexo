package io.github.caiomc.nexo.adapters.shared.openai.exception;

public class OpenAiNotConfiguredException extends IllegalStateException {

    public OpenAiNotConfiguredException() {
        super("OPENAI_API_KEY não configurada. Copie .env.example para .env na raiz do repositório e preencha a chave.");
    }
}
