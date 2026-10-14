package io.github.caiomc.nexo.core.conversation.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.ConversationView;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ConversationData;
import io.github.caiomc.nexo.core.conversation.usecase.command.CreateConversationCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * {@link ConversationData} ⇄ {@link Conversation}.
 */
@Component
public class ConversationDataMapper {

    public Conversation toConversation(ConversationData data) {
        return new Conversation(
                data.id(),
                UserId.of(data.userId()),
                data.title(),
                data.lastSeq(),
                data.createdAt(),
                data.updatedAt()
        );
    }

    public ConversationData toData(Conversation conversation) {
        return new ConversationData(
                conversation.id(),
                conversation.userId().value(),
                conversation.title(),
                conversation.lastSeq(),
                conversation.createdAt(),
                conversation.updatedAt()
        );
    }

    public Conversation newConversation(
            CreateConversationCommand command,
            Instant now
    ) {
        return Conversation.start(command.userId(), command.title(), now);
    }

    public ConversationView toView(
            Conversation conversation,
            Turn activeTurn
    ) {
        return new ConversationView(conversation, activeTurn);
    }
}
