package io.github.caiomc.nexo.adapters.conversation.openai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.caiomc.nexo.adapters.shared.openai.OpenAiHttpClient;
import io.github.caiomc.nexo.adapters.shared.openai.exception.OpenAiApiException;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelDeltaData;

import static java.util.Objects.nonNull;

/**
 * A chamada HTTP de fato: {@code POST /responses} em streaming (SSE). Entrega cada
 * {@code response.output_text.delta}, devolve a resposta completa e tenta de novo em LLM 5xx ou
 * falha de rede antes do primeiro pedaço de texto. Um por sessão: guarda o stream aberto para o
 * botão Parar conseguir fechá-lo de outra thread.
 */
public class ResponsesStreamer {

    private static final Logger log = LoggerFactory.getLogger(ResponsesStreamer.class);

    private static final String PATH = "/responses";
    private static final String TEXT_DELTA = "response.output_text.delta";
    private static final int RETRIES = 2;

    private final OpenAiHttpClient http;

    private volatile InputStream current;
    private volatile boolean cancelled;

    public ResponsesStreamer(OpenAiHttpClient http) {
        this.http = http;
    }

    /**
     * @return o objeto {@code response} do evento {@code response.completed}
     */
    public JsonNode stream(
            ObjectNode request,
            Consumer<ModelDeltaData> onDelta
    ) {
        boolean[] streamed = {false};

        for (int attempt = 0; ; attempt++) {
            try {
                return this.http.stream(
                        PATH,
                        request,
                        event -> {
                            if (TEXT_DELTA.equals(event.path("type").asText())) {
                                streamed[0] = true;
                                onDelta.accept(new ModelDeltaData(event.path("delta").asText("")));
                            }
                        },
                        stream -> this.current = stream
                );
            } catch (OpenAiApiException | UncheckedIOException e) {
                boolean retryable = !(e instanceof OpenAiApiException api) || api.status() >= 500;

                if (this.cancelled || streamed[0] || !retryable || attempt >= RETRIES) {
                    throw e;
                }

                log.warn("Chamada ao modelo falhou ({}), tentando de novo", e.getMessage());
            } finally {
                this.current = null;
            }
        }
    }

    /**
     * Botão Parar: fecha o stream em andamento e não tenta de novo.
     */
    public void cancel() {
        this.cancelled = true;
        this.close();
    }

    public void close() {
        InputStream stream = this.current;

        if (nonNull(stream)) {
            try {
                stream.close();
            } catch (IOException e) {
                log.debug("Falha fechando o stream do modelo: {}", e.getMessage());
            }
        }
    }
}
