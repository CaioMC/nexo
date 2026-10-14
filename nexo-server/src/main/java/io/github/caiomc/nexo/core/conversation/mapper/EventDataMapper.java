package io.github.caiomc.nexo.core.conversation.mapper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.domain.enums.EventType;
import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.domain.model.NewEvent;
import io.github.caiomc.nexo.core.conversation.port.event.command.AppendEventCommand;
import io.github.caiomc.nexo.core.conversation.port.event.data.EventData;

/**
 * {@link NewEvent} → gravação; {@link EventData} → {@link AssistantEvent}.
 */
@Component
public class EventDataMapper {

    public AppendEventCommand toAppend(
            UUID conversationId,
            NewEvent event,
            Instant at
    ) {
        return new AppendEventCommand(
                conversationId,
                event.turnId(),
                event.type().wire(),
                event.payload(),
                at
        );
    }

    public AssistantEvent toEvent(EventData data) {
        return new AssistantEvent(
                data.conversationId(),
                data.seq(),
                data.turnId(),
                EventType.fromWire(data.type()),
                data.payload(),
                data.createdAt()
        );
    }
}
