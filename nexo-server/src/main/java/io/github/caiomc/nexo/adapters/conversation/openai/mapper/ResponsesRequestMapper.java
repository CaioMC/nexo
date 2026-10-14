package io.github.caiomc.nexo.adapters.conversation.openai.mapper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;
import io.github.caiomc.nexo.core.conversation.port.model.command.HistoryMessageCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.OpenModelSessionCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;

/**
 * Monta o corpo do {@code POST /responses}: a parte fixa do turno (modelo, instruções,
 * {@code tools}) e os itens do {@code input} (histórico, mensagem do usuário, respostas aos pedidos
 * do modelo e os itens que o próprio modelo produziu).
 */
public class ResponsesRequestMapper {

    private final ObjectMapper json;
    private final ResponsesToolMapper tools;

    public ResponsesRequestMapper(
            ObjectMapper json,
            ResponsesToolMapper tools
    ) {
        this.json = json;
        this.tools = tools;
    }

    /**
     * O começo do prompt, que não muda durante o turno (o cache de prompt continua valendo).
     */
    public ObjectNode toBaseRequest(
            String model,
            OpenModelSessionCommand command
    ) {
        ObjectNode request = this.json.createObjectNode()
                .put("model", model)
                .put("instructions", command.instructions())
                .put("stream", true);
        ArrayNode functions = request.putArray("tools");

        if (command.exposure() == ToolExposure.CLIENT_TOOL_SEARCH) {
            functions.add(this.tools.clientToolSearch(command.namespaces()));
        }

        functions.addAll(this.tools.toFunctions(command.preloadedTools(), false));

        return request;
    }

    /**
     * O {@code input} da 1ª chamada: as mensagens dos turnos anteriores e a mensagem do usuário.
     */
    public ArrayNode toInitialInput(OpenModelSessionCommand command) {
        ArrayNode input = this.json.createArrayNode();

        for (HistoryMessageCommand message : command.history()) {
            input.add(this.message(message.role(), message.text()));
        }

        input.add(this.message(HistoryMessageCommand.USER, command.userMessage()));

        return input;
    }

    /**
     * A resposta do servidor a um pedido do modelo, no mesmo {@code call_id}. As ferramentas achadas
     * pelo tool search entram aqui, no fim do histórico, com {@code defer_loading}.
     */
    public ObjectNode toInputItem(ToolOutputCommand output) {
        return switch (output) {
            case ToolOutputCommand.SearchResult(String callId, var found) -> {
                ObjectNode item = this.json.createObjectNode()
                        .put("type", "tool_search_output")
                        .put("call_id", callId)
                        .put("execution", "client")
                        .put("status", "completed");

                item.set("tools", this.tools.toFunctions(found, true));

                yield item;
            }
            case ToolOutputCommand.FunctionResult(String callId, String text) -> this.json.createObjectNode()
                    .put("type", "function_call_output")
                    .put("call_id", callId)
                    .put("output", text);
        };
    }

    /**
     * O corpo de uma chamada: a parte fixa com o {@code input} acumulado até agora.
     */
    public ObjectNode toRequest(
            ObjectNode baseRequest,
            ArrayNode input
    ) {
        ObjectNode request = baseRequest.deepCopy();

        request.set("input", input.deepCopy());

        return request;
    }

    /**
     * Um item do {@code output} do modelo, pronto para ser reenviado no {@code input}. Proxies
     * compatíveis acrescentam campos nulos que a API recusa na volta: todos os nulos saem.
     */
    public JsonNode toReplayableItem(JsonNode outputItem) {
        JsonNode copy = outputItem.deepCopy();

        this.removeNulls(copy);

        return copy;
    }

    private ObjectNode message(
            String role,
            String text
    ) {
        return this.json.createObjectNode().put("role", role).put("content", text);
    }

    private void removeNulls(JsonNode node) {
        if (node instanceof ObjectNode object) {
            List<String> nulls = new ArrayList<>();

            object.properties().forEach(field -> {
                if (field.getValue().isNull()) {
                    nulls.add(field.getKey());
                } else {
                    this.removeNulls(field.getValue());
                }
            });
            object.remove(nulls);
        }

        if (node instanceof ArrayNode array) {
            array.forEach(this::removeNulls);
        }
    }
}
