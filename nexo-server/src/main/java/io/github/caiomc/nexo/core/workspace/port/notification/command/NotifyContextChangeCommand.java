package io.github.caiomc.nexo.core.workspace.port.notification.command;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

public record NotifyContextChangeCommand(
        UserId userId,
        ContextKey context,
        boolean enabled
) {
}
