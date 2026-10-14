package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.domain.exception.TurnInProgressException;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.ConversationDataMapper;
import io.github.caiomc.nexo.core.conversation.port.repository.ConversationRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.conversation.usecase.CreateConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.DeleteConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.RenameConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.CreateConversationCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.DeleteConversationCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.RenameConversationCommand;

/**
 * Criar, renomear e apagar conversas (RF-20).
 */
@Service
public class ConversationCommandService implements CreateConversationUseCase, RenameConversationUseCase, DeleteConversationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConversationCommandService.class);

    private final ConversationRepository conversations;
    private final TurnRepository turns;
    private final ConversationQueryService queries;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;
    private final ConversationDataMapper mapper;
    private final Clock clock;

    public ConversationCommandService(
            ConversationRepository conversations,
            TurnRepository turns,
            ConversationQueryService queries,
            ConversationEventHubService hub,
            ConversationEvents events,
            ConversationDataMapper mapper,
            Clock clock
    ) {
        this.conversations = conversations;
        this.turns = turns;
        this.queries = queries;
        this.hub = hub;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public Conversation create(CreateConversationCommand command) {
        Conversation conversation = this.mapper.newConversation(command, this.clock.instant());

        this.conversations.save(this.mapper.toData(conversation));
        log.info("Conversa {} criada para {}", conversation.id(), command.userId());

        return conversation;
    }

    @Override
    public Conversation rename(RenameConversationCommand command) {
        Conversation renamed = this.queries.owned(command.userId(), command.conversationId()).renamed(command.title(), this.clock.instant());

        this.conversations.save(this.mapper.toData(renamed));
        this.hub.publish(renamed.id(), this.events.conversationRenamed(renamed.title()));

        return this.queries.owned(command.userId(), command.conversationId());
    }

    /**
     * Com turno ativo não apaga (409): o usuário para o turno antes.
     */
    @Override
    public void delete(DeleteConversationCommand command) {
        Conversation conversation = this.queries.owned(command.userId(), command.conversationId());

        if (this.turns.findActive(conversation.id()).isPresent()) {
            throw new TurnInProgressException(conversation.id());
        }

        this.conversations.delete(conversation.id());
        this.hub.terminate(conversation.id());
        log.info("Conversa {} apagada", conversation.id());
    }
}
