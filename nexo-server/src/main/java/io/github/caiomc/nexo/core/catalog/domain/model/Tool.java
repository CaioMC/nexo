package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.Comparator;
import java.util.List;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;

/**
 * Uma ferramenta do catálogo: o que o modelo pode chamar, de qual contexto Google ela é e o que
 * ela faz. Os pré-requisitos ficam no {@link DependencyGraph}, não aqui.
 */
public record Tool(
        ToolName name,
        String namespace,
        ContextKey context,
        ToolEffect effect,
        String description,
        List<ToolParameter> parameters
) {

    public Tool {
        if (isNull(name) || isNull(context) || isNull(effect)) {
            throw new IllegalArgumentException("Ferramenta precisa de nome, contexto e efeito: " + name);
        }

        if (isNull(namespace) || namespace.isBlank()) {
            throw new IllegalArgumentException("Ferramenta " + name + " sem namespace");
        }

        if (isNull(description) || description.isBlank()) {
            throw new IllegalArgumentException("Ferramenta " + name + " sem descrição: o RAG precisa dela");
        }

        parameters = isNull(parameters) ?
                List.of() :
                parameters.stream().sorted(Comparator.comparingInt(ToolParameter::ordinal)).toList();
    }
}
