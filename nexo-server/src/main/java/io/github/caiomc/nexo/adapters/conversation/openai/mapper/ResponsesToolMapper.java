package io.github.caiomc.nexo.adapters.conversation.openai.mapper;

import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;

import static java.util.Objects.nonNull;

/**
 * Ferramentas do catálogo → campo {@code tools} e itens {@code tool_search_output} da Responses API
 * (mesmo formato do dta-tool-search).
 */
public class ResponsesToolMapper {

    private final ObjectMapper json;
    private final ProvideCatalogUseCase catalog;

    public ResponsesToolMapper(
            ObjectMapper json,
            ProvideCatalogUseCase catalog
    ) {
        this.json = json;
        this.catalog = catalog;
    }

    /**
     * Modo cliente: o modelo chama esta "ferramenta de busca" com um objetivo e o servidor responde
     * com as funções encontradas. A descrição cita os namespaces dos contextos ligados.
     */
    public ObjectNode clientToolSearch(List<String> namespaces) {
        String available = namespaces.isEmpty() ? "nenhum (o usuário não ligou contextos)" : String.join(", ", namespaces);
        ObjectNode search = this.json.createObjectNode()
                .put("type", "tool_search")
                .put("execution", "client")
                .put("description", "Busca no catálogo do Google Workspace as ferramentas necessárias para a tarefa, inclusive as que criam, alteram ou apagam. Use sempre que a ferramenta certa ainda não estiver carregada. Descreva em 'goal' o que precisa ser feito. Namespaces disponíveis: " + available + ".");
        ObjectNode parameters = search.putObject("parameters").put("type", "object");

        parameters.putObject("properties").putObject("goal").put("type", "string").put("description", "Objetivo em linguagem natural, ex.: listar os eventos de amanhã na agenda.");
        parameters.putArray("required").add("goal");
        parameters.put("additionalProperties", false);

        return search;
    }

    public ArrayNode toFunctions(
            List<Tool> tools,
            boolean deferred
    ) {
        ArrayNode functions = this.json.createArrayNode();

        tools.forEach(tool -> functions.add(this.toFunction(tool, deferred)));

        return functions;
    }

    /**
     * {@code strict} desligado: o tool search não aceita {@code strict: true} e há parâmetros opcionais.
     */
    public ObjectNode toFunction(
            Tool tool,
            boolean deferred
    ) {
        ObjectNode function = this.json.createObjectNode()
                .put("type", "function")
                .put("name", tool.name().value())
                .put("description", this.descriptionDAG(tool))
                .put("strict", false);

        if (deferred) {
            function.put("defer_loading", true);
        }

        function.set("parameters", this.schema(tool.parameters()));

        return function;
    }

    /**
     * O DAG também informa o modelo: os pré-requisitos vão na descrição. Alterações avisam que
     * pedem confirmação.
     */
    private String descriptionDAG(Tool tool) {
        SortedSet<ToolName> prerequisites = this.catalog.current().graph().prerequisitesOfDAG(tool.name());
        String text = tool.description();

        if (!prerequisites.isEmpty()) {
            text += " Pré-requisitos (executar antes): " + prerequisites.stream().map(ToolName::value).collect(Collectors.joining(", ")) + ".";
        }

        if (tool.effect().requiresApproval()) {
            text += " Altera dados: o usuário confirma antes de executar.";
        }

        return text;
    }

    private ObjectNode schema(List<ToolParameter> parameters) {
        ObjectNode schema = this.json.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (ToolParameter parameter : parameters) {
            ObjectNode property = properties.putObject(parameter.name())
                    .put("type", parameter.type().name().toLowerCase(Locale.ROOT))
                    .put("description", parameter.description());

            if (nonNull(parameter.minimum())) {
                property.put("minimum", parameter.minimum());
            }

            if (nonNull(parameter.maximum())) {
                property.put("maximum", parameter.maximum());
            }

            if (!parameter.enumValues().isEmpty()) {
                ArrayNode values = property.putArray("enum");

                parameter.enumValues().forEach(values::add);
            }

            if (nonNull(parameter.itemsSchema())) {
                property.set("items", this.items(parameter.itemsSchema()));
            }

            if (parameter.required()) {
                required.add(parameter.name());
            }
        }

        schema.put("additionalProperties", false);

        return schema;
    }

    private ObjectNode items(String itemsSchema) {
        try {
            return (ObjectNode) this.json.readTree(itemsSchema);
        } catch (JsonProcessingException | ClassCastException e) {
            return this.json.createObjectNode().put("type", "string");
        }
    }
}
