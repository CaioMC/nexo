package io.github.caiomc.nexo.core.workspace.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * @param context chave como veio da URL; o serviço confere se existe
 */
public record EnableContextCommand(
        UserId userId,
        String context
) {

    public EnableContextCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }
    }
}
