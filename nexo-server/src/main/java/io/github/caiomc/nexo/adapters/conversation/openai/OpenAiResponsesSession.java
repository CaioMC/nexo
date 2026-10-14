package io.github.caiomc.nexo.adapters.conversation.openai;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesOutputParser;
import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesRequestMapper;
import io.github.caiomc.nexo.core.conversation.port.model.ModelSession;
import io.github.caiomc.nexo.core.conversation.port.model.command.ModelCallCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.OpenModelSessionCommand;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelDeltaData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelOutputData;

/**
 * Um turno na Responses API, sem estado no servidor da OpenAI: cada {@link #call} reenvia o
 * {@code input} inteiro com os itens que o modelo produziu e as respostas do servidor. As
 * ferramentas carregadas pelo tool search entram no fim do histórico ({@code tool_search_output}),
 * então o começo do prompt (instruções + {@code tools}) não muda e o cache de prompt continua valendo.
 *
 * <p>Uma chamada: acrescenta as respostas → monta o corpo ({@link ResponsesRequestMapper}) →
 * envia em streaming ({@link ResponsesStreamer}) → guarda o {@code output} no histórico → traduz
 * ({@link ResponsesOutputParser}).
 */
public class OpenAiResponsesSession implements ModelSession {

    private final ResponsesStreamer streamer;
    private final ResponsesRequestMapper requests;
    private final ResponsesOutputParser parser;

    private final ObjectNode baseRequest;
    private final ArrayNode input;

    public OpenAiResponsesSession(
            ResponsesStreamer streamer,
            ResponsesRequestMapper requests,
            ResponsesOutputParser parser,
            String model,
            OpenModelSessionCommand command
    ) {
        this.streamer = streamer;
        this.requests = requests;
        this.parser = parser;
        this.baseRequest = requests.toBaseRequest(model, command);
        this.input = requests.toInitialInput(command);
    }

    @Override
    public ModelOutputData call(
            ModelCallCommand command,
            Consumer<ModelDeltaData> onDelta
    ) {
        command.toolOutputs().forEach(output -> this.input.add(this.requests.toInputItem(output)));

        JsonNode response = this.streamer.stream(
                this.requests.toRequest(this.baseRequest, this.input),
                onDelta
        );

        response.path("output").forEach(item -> this.input.add(this.requests.toReplayableItem(item)));

        return this.parser.toOutput(response);
    }

    @Override
    public void cancel() {
        this.streamer.cancel();
    }

    @Override
    public void close() {
        this.streamer.close();
    }
}
