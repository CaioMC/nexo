package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

import static java.util.Objects.isNull;

/**
 * Um parâmetro de ferramenta: vira uma propriedade do JSON Schema da função.
 *
 * @param itemsSchema JSON Schema dos itens quando o tipo é {@link ParameterType#ARRAY} (texto JSON)
 */
public record ToolParameter(
        int ordinal,
        String name,
        ParameterType type,
        String description,
        boolean required,
        Double minimum,
        Double maximum,
        List<String> enumValues,
        String itemsSchema
) {

    public ToolParameter {
        if (isNull(name) || name.isBlank()) {
            throw new IllegalArgumentException("Parâmetro sem nome na posição " + ordinal);
        }

        if (isNull(type)) {
            throw new IllegalArgumentException("Parâmetro " + name + " sem tipo");
        }

        if ((type == ParameterType.ARRAY) == isNull(itemsSchema)) {
            throw new IllegalArgumentException("Parâmetro " + name + ": o esquema dos itens é obrigatório só em array");
        }

        description = isNull(description) ? "" : description;
        enumValues = isNull(enumValues) ? List.of() : List.copyOf(enumValues);
    }
}
