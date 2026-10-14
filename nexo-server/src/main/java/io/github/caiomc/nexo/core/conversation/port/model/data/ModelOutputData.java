package io.github.caiomc.nexo.core.conversation.port.model.data;

import java.util.List;

import static java.util.Objects.isNull;

/**
 * O que o modelo devolveu numa chamada. Ou é a resposta final (só texto), ou traz pedidos ao
 * servidor: buscas de ferramentas e/ou chamadas de função, que podem vir juntas e acompanhadas de texto.
 */
public record ModelOutputData(
        String text,
        List<ToolSearchCallData> toolSearches,
        List<FunctionCallData> functionCalls,
        UsageData usage
) {

    public ModelOutputData {
        text = isNull(text) ? "" : text;
        toolSearches = isNull(toolSearches) ? List.of() : List.copyOf(toolSearches);
        functionCalls = isNull(functionCalls) ? List.of() : List.copyOf(functionCalls);
    }

    /**
     * O modelo não pediu nada: o texto é a resposta final do turno.
     */
    public boolean isFinalAnswer() {
        return this.toolSearches.isEmpty() && this.functionCalls.isEmpty();
    }
}
