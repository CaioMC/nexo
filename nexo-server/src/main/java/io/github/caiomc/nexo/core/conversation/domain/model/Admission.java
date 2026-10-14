package io.github.caiomc.nexo.core.conversation.domain.model;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;

/**
 * A resposta do {@code ToolGatekeeperService} para uma chamada proposta pelo modelo (doc 11, seção 4).
 */
public sealed interface Admission {

    static Admission unknown(String name) {
        return new Unknown("Ferramenta desconhecida: " + name + ". Use tool_search para obter as ferramentas disponíveis.");
    }

    static Admission invalid(
            Tool tool,
            List<String> errors
    ) {
        return new Invalid(tool, errors);
    }

    static Admission blocked(
            Tool tool,
            BlockReason reason,
            List<String> missing
    ) {
        return new Blocked(tool, reason, missing);
    }

    static Admission run(
            Tool tool,
            ExecuteToolCommand command
    ) {
        return new Run(tool, command);
    }

    static Admission awaitApproval(
            Tool tool,
            ToolApproval approval,
            ExecuteToolCommand command
    ) {
        return new AwaitApproval(tool, approval, command);
    }

    /**
     * 1: não existe no catálogo. Erro ao modelo, sem evento.
     */
    record Unknown(String message) implements Admission {
    }

    /**
     * 3: argumentos fora do esquema. Erro de validação ao modelo.
     */
    record Invalid(
            Tool tool,
            List<String> errors
    ) implements Admission {
    }

    /**
     * 2 e 4: contexto desligado ou pré-requisito faltando. {@code tool.blocked}.
     */
    record Blocked(
            Tool tool,
            BlockReason reason,
            List<String> missing
    ) implements Admission {
    }

    /**
     * 5: leitura, executa direto (RF-34).
     */
    record Run(
            Tool tool,
            ExecuteToolCommand command
    ) implements Admission {
    }

    /**
     * 5: alteração, pausa até o usuário decidir (RF-35).
     */
    record AwaitApproval(
            Tool tool,
            ToolApproval approval,
            ExecuteToolCommand command
    ) implements Admission {
    }
}
