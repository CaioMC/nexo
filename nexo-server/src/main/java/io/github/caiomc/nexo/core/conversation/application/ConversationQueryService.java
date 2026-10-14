package io.github.caiomc.nexo.core.conversation.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.domain.exception.ConversationNotFoundException;
import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.ConversationView;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.mapper.ConversationDataMapper;
import io.github.caiomc.nexo.core.conversation.mapper.EventDataMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ToolApprovalDataMapper;
import io.github.caiomc.nexo.core.conversation.mapper.TurnDataMapper;
import io.github.caiomc.nexo.core.conversation.port.event.EventStorePort;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.ConversationRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.conversation.usecase.GetConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.ListApprovalsUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.ListConversationsUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.ReadEventsUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.GetConversationCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.ListApprovalsCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.ListConversationsCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.ReadEventsCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Consultas de conversa. Todas passam por {@link #owned}: um usuário só enxerga as próprias
 * conversas, eventos e aprovações (RN-10).
 */
@Service
public class ConversationQueryService implements ListConversationsUseCase, GetConversationUseCase, ReadEventsUseCase, ListApprovalsUseCase {

    private final ConversationRepository conversations;
    private final TurnRepository turns;
    private final ApprovalRepository approvals;
    private final EventStorePort events;

    private final ConversationDataMapper conversationMapper;
    private final TurnDataMapper turnMapper;
    private final ToolApprovalDataMapper approvalMapper;
    private final EventDataMapper eventMapper;

    public ConversationQueryService(
            ConversationRepository conversations,
            TurnRepository turns,
            ApprovalRepository approvals,
            EventStorePort events,
            ConversationDataMapper conversationMapper,
            TurnDataMapper turnMapper,
            ToolApprovalDataMapper approvalMapper,
            EventDataMapper eventMapper
    ) {
        this.conversations = conversations;
        this.turns = turns;
        this.approvals = approvals;
        this.events = events;
        this.conversationMapper = conversationMapper;
        this.turnMapper = turnMapper;
        this.approvalMapper = approvalMapper;
        this.eventMapper = eventMapper;
    }

    /**
     * A conversa, se for do usuário. De outro usuário ou inexistente: a mesma exceção.
     */
    public Conversation owned(
            UserId userId,
            UUID conversationId
    ) {
        return this.conversations.findById(conversationId)
                .map(this.conversationMapper::toConversation)
                .filter(conversation -> conversation.belongsTo(userId))
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    @Override
    public List<Conversation> list(ListConversationsCommand command) {
        return this.conversations.findByUser(command.userId().value(), command.before(), command.limit())
                .stream()
                .map(this.conversationMapper::toConversation)
                .toList();
    }

    @Override
    public ConversationView get(GetConversationCommand command) {
        Conversation conversation = this.owned(command.userId(), command.conversationId());

        return this.conversationMapper.toView(
                conversation,
                this.turns.findActive(conversation.id()).map(this.turnMapper::toTurn).orElse(null)
        );
    }

    @Override
    public List<AssistantEvent> read(ReadEventsCommand command) {
        Conversation conversation = this.owned(command.userId(), command.conversationId());

        return this.events.findAfter(conversation.id(), command.afterSeq(), command.limit())
                .stream()
                .map(this.eventMapper::toEvent)
                .toList();
    }

    @Override
    public List<ToolApproval> list(ListApprovalsCommand command) {
        Conversation conversation = this.owned(command.userId(), command.conversationId());

        return this.approvals.findByConversation(conversation.id(), isNull(command.status()) ? null : command.status().name())
                .stream()
                .map(this.approvalMapper::toApproval)
                .toList();
    }
}
