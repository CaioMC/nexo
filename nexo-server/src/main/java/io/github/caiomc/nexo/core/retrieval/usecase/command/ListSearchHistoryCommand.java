package io.github.caiomc.nexo.core.retrieval.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record ListSearchHistoryCommand(
        UserId userId,
        int limit
) {

    public ListSearchHistoryCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit precisa estar entre 1 e 100, veio " + limit);
        }
    }
}
