package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.usecase.command.PublishContextChangeCommand;

public interface PublishContextChangeUseCase {

    /**
     * {@code context.changed} nas conversas abertas do usuário (RF-14).
     */
    void publish(PublishContextChangeCommand command);
}
