package io.github.caiomc.nexo.core.conversation.port.event;

import java.util.List;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.port.event.command.AppendEventCommand;
import io.github.caiomc.nexo.core.conversation.port.event.data.EventData;

/**
 * Event Store da conversa: cada evento ganha um {@code seq} crescente, sem buraco.
 */
public interface EventStorePort {

    EventData append(AppendEventCommand command);

    List<EventData> findAfter(
            UUID conversationId,
            long afterSeq,
            int limit
    );
}
