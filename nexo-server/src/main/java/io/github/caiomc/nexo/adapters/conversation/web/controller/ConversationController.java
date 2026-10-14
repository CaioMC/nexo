package io.github.caiomc.nexo.adapters.conversation.web.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.caiomc.nexo.adapters.conversation.web.mapper.ConversationWebMapper;
import io.github.caiomc.nexo.adapters.conversation.web.request.CreateConversationRequest;
import io.github.caiomc.nexo.adapters.conversation.web.request.RenameConversationRequest;
import io.github.caiomc.nexo.adapters.conversation.web.response.ApprovalResponse;
import io.github.caiomc.nexo.adapters.conversation.web.response.ConversationResponse;
import io.github.caiomc.nexo.adapters.conversation.websocket.message.EventMessage;
import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.conversation.usecase.CreateConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.DeleteConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.GetConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.ListApprovalsUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.ListConversationsUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.ReadEventsUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.RenameConversationUseCase;

/**
 * Conversas (RF-20). Mensagens, aprovações e parada vão pelo WebSocket: não há rota REST
 * equivalente, para não existirem dois caminhos com regras diferentes.
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final CreateConversationUseCase create;
    private final ListConversationsUseCase list;
    private final GetConversationUseCase get;
    private final RenameConversationUseCase rename;
    private final DeleteConversationUseCase delete;
    private final ReadEventsUseCase events;
    private final ListApprovalsUseCase approvals;
    private final ConversationWebMapper mapper;

    public ConversationController(
            CreateConversationUseCase create,
            ListConversationsUseCase list,
            GetConversationUseCase get,
            RenameConversationUseCase rename,
            DeleteConversationUseCase delete,
            ReadEventsUseCase events,
            ListApprovalsUseCase approvals,
            ConversationWebMapper mapper
    ) {
        this.create = create;
        this.list = list;
        this.get = get;
        this.rename = rename;
        this.delete = delete;
        this.events = events;
        this.approvals = approvals;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(
            @AuthenticationPrincipal SessionUser user,
            @RequestBody(required = false) CreateConversationRequest request
    ) {
        return this.mapper.toResponse(
                this.create.create(
                        this.mapper.toCreateCommand(user, request)
                )
        );
    }

    @GetMapping
    public List<ConversationResponse> list(
            @AuthenticationPrincipal SessionUser user,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before
    ) {
        return this.mapper.toResponses(
                this.list.list(
                        this.mapper.toListCommand(user, limit, before)
                )
        );
    }

    @GetMapping("/{id}")
    public ConversationResponse get(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID id
    ) {
        return this.mapper.toResponse(
                this.get.get(
                        this.mapper.toGetCommand(user, id)
                )
        );
    }

    @PatchMapping("/{id}")
    public ConversationResponse rename(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID id,
            @RequestBody(required = false) RenameConversationRequest request
    ) {
        return this.mapper.toResponse(
                this.rename.rename(
                        this.mapper.toRenameCommand(user, id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID id
    ) {
        this.delete.delete(this.mapper.toDeleteCommand(user, id));
    }

    @GetMapping("/{id}/events")
    public List<EventMessage> events(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID id,
            @RequestParam(required = false) Long afterSeq,
            @RequestParam(required = false) Integer limit
    ) {
        return this.mapper.toEventResponses(
                this.events.read(
                        this.mapper.toEventsCommand(
                                user,
                                id,
                                afterSeq,
                                limit
                        )
                )
        );
    }

    @GetMapping("/{id}/approvals")
    public List<ApprovalResponse> approvals(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID id,
            @RequestParam(required = false) String status
    ) {
        return this.mapper.toApprovalResponses(
                this.approvals.list(
                        this.mapper.toApprovalsCommand(user, id, status)
                )
        );
    }
}
