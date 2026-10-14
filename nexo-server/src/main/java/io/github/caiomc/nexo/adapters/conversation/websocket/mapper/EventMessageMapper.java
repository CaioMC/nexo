package io.github.caiomc.nexo.adapters.conversation.websocket.mapper;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.conversation.websocket.message.AckMessage;
import io.github.caiomc.nexo.adapters.conversation.websocket.message.ClientCommandMessage;
import io.github.caiomc.nexo.adapters.conversation.websocket.message.EventMessage;
import io.github.caiomc.nexo.adapters.conversation.websocket.message.NackMessage;
import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalDecision;
import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.usecase.command.DecideApprovalCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.SendMessageCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.StopTurnCommand;
import io.github.caiomc.nexo.core.conversation.usecase.command.WatchConversationCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Formato do fio (doc 06) ⇄ comandos e eventos do core. Validação de formato fica aqui.
 */
@Component
public class EventMessageMapper {

    public EventMessage toMessage(AssistantEvent event) {
        return new EventMessage(
                EventMessage.EVENT,
                event.seq(),
                event.conversationId(),
                event.turnId(),
                event.type().wire(),
                event.at(),
                event.payload()
        );
    }

    public AckMessage toAck(
            String commandId,
            UUID turnId
    ) {
        return new AckMessage(AckMessage.ACK, commandId, turnId);
    }

    public NackMessage toNack(
            String commandId,
            String code,
            String message
    ) {
        return new NackMessage(
                NackMessage.NACK,
                commandId,
                code,
                message
        );
    }

    public WatchConversationCommand toWatchCommand(
            SessionUser user,
            UUID conversationId,
            long lastSeq
    ) {
        return new WatchConversationCommand(
                UserId.of(user.userId()),
                conversationId,
                lastSeq
        );
    }

    /**
     * Em {@code user.message} o {@code id} do comando é o {@code clientMessageId} (RF-26).
     */
    public SendMessageCommand toSendCommand(
            SessionUser user,
            UUID conversationId,
            ClientCommandMessage message
    ) {
        return new SendMessageCommand(
                UserId.of(user.userId()),
                conversationId,
                this.uuid(message.id(), "id"),
                this.text(message.payload(), "text"),
                this.text(message.payload(), "timeZone")
        );
    }

    public DecideApprovalCommand toDecideCommand(
            SessionUser user,
            UUID conversationId,
            ClientCommandMessage message
    ) {
        return new DecideApprovalCommand(
                UserId.of(user.userId()),
                conversationId,
                this.uuid(this.text(message.payload(), "approvalId"), "approvalId"),
                ApprovalDecision.of(this.text(message.payload(), "decision"))
        );
    }

    public StopTurnCommand toStopCommand(
            SessionUser user,
            UUID conversationId
    ) {
        return new StopTurnCommand(UserId.of(user.userId()), conversationId);
    }

    private String text(
            Map<String, Object> payload,
            String name
    ) {
        if (isNull(payload) || isNull(payload.get(name))) {
            return null;
        }

        if (!(payload.get(name) instanceof String value)) {
            throw new IllegalArgumentException(name + " precisa ser texto");
        }

        return value;
    }

    private UUID uuid(
            String value,
            String name
    ) {
        if (isNull(value)) {
            throw new IllegalArgumentException("Informe " + name + " (UUID)");
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " precisa ser um UUID: " + value);
        }
    }
}
