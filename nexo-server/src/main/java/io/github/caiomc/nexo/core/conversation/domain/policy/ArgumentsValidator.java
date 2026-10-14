package io.github.caiomc.nexo.core.conversation.domain.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Regra 3 do gatekeeper: os argumentos batem com o esquema do catálogo (tipos, obrigatórios,
 * {@code minimum}/{@code maximum}, {@code enum})? Valor nulo conta como "não informado".
 */
public class ArgumentsValidator {

    public List<String> validate(
            Tool tool,
            Map<String, Object> arguments
    ) {
        List<String> errors = new ArrayList<>();

        for (ToolParameter parameter : tool.parameters()) {
            Object value = arguments.get(parameter.name());

            if (!arguments.containsKey(parameter.name())) {
                if (parameter.required()) {
                    errors.add(parameter.name() + ": obrigatório");
                }

                continue;
            }

            if (isNull(value)) {
                continue;
            }

            this.check(parameter, value, errors);
        }

        for (String name : arguments.keySet()) {
            if (tool.parameters().stream().noneMatch(parameter -> parameter.name().equals(name))) {
                errors.add(name + ": parâmetro desconhecido");
            }
        }

        return errors;
    }

    private void check(
            ToolParameter parameter,
            Object value,
            List<String> errors
    ) {
        String name = parameter.name();

        switch (parameter.type()) {
            case STRING -> {
                if (!(value instanceof String text)) {
                    errors.add(name + ": esperava texto");
                } else if (!parameter.enumValues().isEmpty() && !parameter.enumValues().contains(text)) {
                    errors.add(name + ": use um de " + parameter.enumValues());
                }
            }
            case INTEGER -> {
                if (!(value instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
                    errors.add(name + ": esperava inteiro");
                } else {
                    this.range(parameter, number, errors);
                }
            }
            case NUMBER -> {
                if (!(value instanceof Number number)) {
                    errors.add(name + ": esperava número");
                } else {
                    this.range(parameter, number, errors);
                }
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    errors.add(name + ": esperava true ou false");
                }
            }
            case ARRAY -> {
                if (!(value instanceof List<?>)) {
                    errors.add(name + ": esperava lista");
                }
            }
        }
    }

    private void range(
            ToolParameter parameter,
            Number number,
            List<String> errors
    ) {
        double value = number.doubleValue();

        if (nonNull(parameter.minimum()) && value < parameter.minimum()) {
            errors.add(parameter.name() + ": mínimo " + parameter.minimum().longValue());
        }

        if (nonNull(parameter.maximum()) && value > parameter.maximum()) {
            errors.add(parameter.name() + ": máximo " + parameter.maximum().longValue());
        }
    }
}
