package io.github.caiomc.nexo.core.workspace.port.notification;

import io.github.caiomc.nexo.core.workspace.port.notification.command.NotifyContextChangeCommand;

/**
 * Avisa as conversas abertas do usuário que um contexto mudou (RF-14).
 */
public interface ContextChangeNotifierPort {

    void notify(NotifyContextChangeCommand command);
}
