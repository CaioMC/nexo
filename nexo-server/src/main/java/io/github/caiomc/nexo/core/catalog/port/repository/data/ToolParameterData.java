package io.github.caiomc.nexo.core.catalog.port.repository.data;

import java.util.List;

/**
 * Uma linha de {@code tool_parameter}, como veio do banco.
 *
 * @param items JSON Schema dos itens (texto) quando o tipo é array; nulo nos demais
 */
public record ToolParameterData(
        String toolName,
        int ordinal,
        String name,
        String type,
        String description,
        boolean required,
        Double minimum,
        Double maximum,
        List<String> enumValues,
        String items
) {
}
