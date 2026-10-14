package io.github.caiomc.nexo.core.conversation.mapper;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;

/**
 * A chamada que o modelo devolveu ({@link FunctionCallData}) → o que vai ao executor.
 */
@Component
public class ExecuteToolCommandMapper {

    public ExecuteToolCommand toCommand(
            Turn turn,
            Tool tool,
            FunctionCallData call
    ) {
        return new ExecuteToolCommand(
                turn.userId(),
                tool.name(),
                tool.context(),
                call.arguments()
        );
    }
}
