package io.github.caiomc.nexo.adapters.conversation.web.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.conversation.web.request.CreateConversationRequest;
import io.github.caiomc.nexo.adapters.conversation.web.request.RenameConversationRequest;
import io.github.caiomc.nexo.adapters.conversation.web.response.ActiveTurnResponse;
import io.github.caiomc.nexo.adapters.conversation.web.response.ApprovalResponse;
import io.github.caiomc.nexo.adapters.conversation.web.response.ConversationResponse;
import io.github.caiomc.nexo.adapters.conversation.websocket.mapper.EventMessageMapper;
import io.github.caiomc.nexo.adapters.conversation.websocket.message.EventMessage;
import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.ConversationView;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.usecase.command.CreateConversationCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.DeleteConversationCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.GetConversationCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.ListApprovalsCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.ListConversationsCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.ReadEventsCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.RenameConversationCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

@Component
public class ConversationWebMapper {

    private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_EVENTS = 500;

    private final EventMessageMapper events;

    public ConversationWebMapper(EventMessageMapper events) {
        this.events = events;
    }

    public CreateConversationCommand toCreateCommand(
            SessionUser user,
            CreateConversationRequest request
    ) {
        return new CreateConversationCommand(this.user(user), isNull(request) ? null : request.title());
    }

    public ListConversationsCommand toListCommand(
            SessionUser user,
            Integer limit,
            Instant before
    ) {
        return new ListConversationsCommand(
                this.user(user),
                before,
                isNull(limit) ? DEFAULT_LIMIT : limit
        );
    }

    public GetConversationCommand toGetCommand(
            SessionUser user,
            UUID id
    ) {
        return new GetConversationCommand(this.user(user), id);
    }

    public RenameConversationCommand toRenameCommand(
            SessionUser user,
            UUID id,
            RenameConversationRequest request
    ) {
        if (isNull(request)) {
            throw new IllegalArgumentException("Corpo ausente: envie {\"title\": \"...\"}");
        }

        return new RenameConversationCommand(
                this.user(user),
                id,
                request.title()
        );
    }

    public DeleteConversationCommand toDeleteCommand(
            SessionUser user,
            UUID id
    ) {
        return new DeleteConversationCommand(this.user(user), id);
    }

    public ReadEventsCommand toEventsCommand(
            SessionUser user,
            UUID id,
            Long afterSeq,
            Integer limit
    ) {
        return new ReadEventsCommand(
                this.user(user),
                id,
                isNull(afterSeq) ? 0 : afterSeq,
                isNull(limit) ? DEFAULT_EVENTS : limit
        );
    }

    public ListApprovalsCommand toApprovalsCommand(
            SessionUser user,
            UUID id,
            String status
    ) {
        return new ListApprovalsCommand(
                this.user(user),
                id,
                this.status(status)
        );
    }

    public ConversationResponse toResponse(Conversation conversation) {
        return this.toResponse(conversation, null);
    }

    public ConversationResponse toResponse(ConversationView view) {
        return this.toResponse(view.conversation(), view.activeTurn());
    }

    public List<ConversationResponse> toResponses(List<Conversation> conversations) {
        return conversations.stream().map(this::toResponse).toList();
    }

    public List<EventMessage> toEventResponses(List<AssistantEvent> events) {
        return events.stream().map(this.events::toMessage).toList();
    }

    public List<ApprovalResponse> toApprovalResponses(List<ToolApproval> approvals) {
        return approvals.stream().map(this::toApprovalResponse).toList();
    }

    private ConversationResponse toResponse(
            Conversation conversation,
            Turn activeTurn
    ) {
        return new ConversationResponse(
                conversation.id(),
                conversation.title(),
                conversation.lastSeq(),
                isNull(activeTurn) ? null : new ActiveTurnResponse(activeTurn.id(), activeTurn.status().name()),
                conversation.createdAt(),
                conversation.updatedAt()
        );
    }

    private ApprovalResponse toApprovalResponse(ToolApproval approval) {
        return new ApprovalResponse(
                approval.id(),
                approval.turnId(),
                approval.callId(),
                approval.tool().value(),
                approval.description(),
                approval.severity().wire(),
                approval.arguments(),
                approval.status().name(),
                approval.expiresAt(),
                approval.decidedAt()
        );
    }

    private ApprovalStatus status(String value) {
        if (isNull(value) || value.isBlank()) {
            return null;
        }

        try {
            return ApprovalStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status desconhecido: '" + value + "'. Use PENDING, APPROVED, REJECTED, EXPIRED ou CONSUMED.");
        }
    }

    private UserId user(SessionUser user) {
        return UserId.of(user.userId());
    }
}
