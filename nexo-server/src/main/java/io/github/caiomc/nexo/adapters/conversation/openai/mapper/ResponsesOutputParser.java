package io.github.caiomc.nexo.adapters.conversation.openai.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelOutputData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ToolSearchCallData;
import io.github.caiomc.nexo.core.conversation.port.model.data.UsageData;

/**
 * O {@code output} de uma resposta da Responses API → o que o modelo pediu:
 * {@code tool_search_call} (modo cliente), {@code function_call} e o texto das {@code message}.
 */
public class ResponsesOutputParser {

    private static final TypeReference<Map<String, Object>> ARGUMENTS = new TypeReference<>() {
    };

    private final ObjectMapper json;

    public ResponsesOutputParser(ObjectMapper json) {
        this.json = json;
    }

    public ModelOutputData toOutput(JsonNode response) {
        List<ToolSearchCallData> toolSearches = new ArrayList<>();
        List<FunctionCallData> functionCalls = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for (JsonNode item : response.path("output")) {
            switch (item.path("type").asText()) {
                case "tool_search_call" -> {
                    if ("client".equals(item.path("execution").asText())) {
                        toolSearches.add(new ToolSearchCallData(item.path("call_id").asText(), this.toolSearchGoal(item.path("arguments"))));
                    }
                }
                case "function_call" -> functionCalls.add(this.toFunctionCall(item));
                case "message" -> item.path("content").forEach(content -> text.append(content.path("text").asText("")));
                default -> {
                }
            }
        }

        return new ModelOutputData(
                text.toString().strip(),
                toolSearches,
                functionCalls,
                this.toUsage(response.path("usage"))
        );
    }

    /**
     * Argumentos que não são JSON válido não derrubam o turno: o gatekeeper devolve o erro ao modelo.
     */
    private FunctionCallData toFunctionCall(JsonNode item) {
        String raw = item.path("arguments").asText("{}");

        try {
            return new FunctionCallData(
                    item.path("call_id").asText(),
                    item.path("name").asText(),
                    this.json.readValue(raw.isBlank() ? "{}" : raw, ARGUMENTS),
                    null
            );
        } catch (JsonProcessingException e) {
            String error = "os argumentos não são um JSON válido: " + raw;

            return new FunctionCallData(
                    item.path("call_id").asText(),
                    item.path("name").asText(),
                    Map.of(),
                    error
            );
        }
    }

    /**
     * O {@code goal} pode vir como objeto ou como texto JSON, conforme o provedor.
     */
    private String toolSearchGoal(JsonNode arguments) {
        if (!arguments.isTextual()) {
            return arguments.path("goal").asText();
        }

        try {
            return this.json.readTree(arguments.asText()).path("goal").asText();
        } catch (JsonProcessingException e) {
            return arguments.asText();
        }
    }

    private UsageData toUsage(JsonNode usage) {
        return new UsageData(
                usage.path("input_tokens").asLong(),
                usage.path("input_tokens_details").path("cached_tokens").asLong(),
                usage.path("output_tokens").asLong()
        );
    }
}
