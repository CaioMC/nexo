package io.github.caiomc.nexo.core.conversation.port.model.data;

import java.util.Map;

import static java.util.Objects.isNull;

/**
 * O modelo quer chamar uma função.
 *
 * @param arguments      argumentos já lidos do JSON (vazio se não era JSON válido)
 * @param argumentsError por que os argumentos não puderam ser lidos; nulo se leu
 */
public record FunctionCallData(
        String callId,
        String name,
        Map<String, Object> arguments,
        String argumentsError
) {

    public FunctionCallData {
        arguments = isNull(arguments) ? Map.of() : arguments;
    }
}
