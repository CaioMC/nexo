package io.github.caiomc.nexo.core.conversation.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record PublishContextChangeCommand(
        UserId userId,
        ContextKey context,
        boolean enabled
) {

    public PublishContextChangeCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }
    }
}
