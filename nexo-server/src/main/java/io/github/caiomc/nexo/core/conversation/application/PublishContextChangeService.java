package io.github.caiomc.nexo.core.conversation.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.usecase.PublishContextChangeUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.PublishContextChangeCommand;

/**
 * RF-14: {@code context.changed} nas conversas que o usuário tem abertas agora.
 */
@Service
public class PublishContextChangeService implements PublishContextChangeUseCase {

    private final ConversationEventHubService hub;
    private final ConversationEvents events;

    public PublishContextChangeService(
            ConversationEventHubService hub,
            ConversationEvents events
    ) {
        this.hub = hub;
        this.events = events;
    }

    @Override
    public void publish(PublishContextChangeCommand command) {
        for (UUID conversationId : this.hub.watchedBy(command.userId())) {
            this.hub.publish(conversationId, this.events.contextChanged(command.context(), command.enabled()));
        }
    }
}
