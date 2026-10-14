package io.github.caiomc.nexo.adapters.workspace.notification;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.usecase.PublishContextChangeUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.PublishContextChangeCommand;
import io.github.caiomc.nexo.core.workspace.port.notification.ContextChangeNotifierPort;
import io.github.caiomc.nexo.core.workspace.port.notification.command.NotifyContextChangeCommand;

/**
 * Contextos avisam as conversas pelo caso de uso delas, sem o core de contextos conhecer o de conversa.
 */
@Component
public class ConversationContextNotifier implements ContextChangeNotifierPort {

    private final PublishContextChangeUseCase publish;

    public ConversationContextNotifier(PublishContextChangeUseCase publish) {
        this.publish = publish;
    }

    @Override
    public void notify(NotifyContextChangeCommand command) {
        this.publish.publish(
                new PublishContextChangeCommand(
                        command.userId(),
                        command.context(),
                        command.enabled()
                )
        );
    }
}
