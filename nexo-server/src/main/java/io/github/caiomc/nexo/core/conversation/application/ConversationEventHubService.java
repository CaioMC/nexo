package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.NewEvent;
import io.github.caiomc.nexo.core.conversation.mapper.EventDataMapper;
import io.github.caiomc.nexo.core.conversation.port.event.EventStorePort;
import io.github.caiomc.nexo.core.conversation.port.event.data.EventData;
import io.github.caiomc.nexo.core.conversation.usecase.ConversationEventListener;
import io.github.caiomc.nexo.core.conversation.usecase.ConversationWatch;
import io.github.caiomc.nexo.core.conversation.usecase.WatchConversationUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.WatchConversationCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * O narrador (mesmo desenho do {@code MissionEventHub} do alien-code): grava cada evento no Event
 * Store, onde ele ganha {@code seq}, e entrega a quem acompanha a conversa.
 *
 * <p>Gravar + distribuir e reproduzir + assinar acontecem sob o mesmo lock por conversa: quem
 * reconecta recebe o histórico e depois o ao vivo, sem buraco nem repetição (RF-22).
 */
@Service
public class ConversationEventHubService implements WatchConversationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConversationEventHubService.class);

    private static final int REPLAY_PAGE = 500;

    private final EventStorePort store;
    private final ConversationQueryService conversations;
    private final EventDataMapper mapper;
    private final Clock clock;

    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<ConversationEventListener>> listeners = new ConcurrentHashMap<>();
    private final Map<UUID, UserId> owners = new ConcurrentHashMap<>();

    public ConversationEventHubService(
            EventStorePort store,
            ConversationQueryService conversations,
            EventDataMapper mapper,
            Clock clock
    ) {
        this.store = store;
        this.conversations = conversations;
        this.mapper = mapper;
        this.clock = clock;
    }

    public AssistantEvent publish(
            UUID conversationId,
            NewEvent event
    ) {
        synchronized (this.lockFor(conversationId)) {
            AssistantEvent stored = this.mapper.toEvent(
                    this.store.append(this.mapper.toAppend(conversationId, event, this.clock.instant()))
            );

            for (ConversationEventListener listener : this.listeners.getOrDefault(conversationId, Set.of())) {
                this.deliver(listener, stored);
            }

            return stored;
        }
    }

    @Override
    public ConversationWatch watch(
            WatchConversationCommand command,
            ConversationEventListener listener
    ) {
        Conversation conversation = this.conversations.owned(command.userId(), command.conversationId());
        UUID id = conversation.id();

        synchronized (this.lockFor(id)) {
            long after = command.lastSeq();
            List<EventData> page;

            do {
                page = this.store.findAfter(id, after, REPLAY_PAGE);

                for (EventData data : page) {
                    this.deliver(listener, this.mapper.toEvent(data));
                    after = data.seq();
                }
            } while (page.size() == REPLAY_PAGE);

            this.listeners.computeIfAbsent(id, key -> new CopyOnWriteArraySet<>()).add(listener);
            this.owners.put(id, conversation.userId());
        }

        return () -> this.unsubscribe(id, listener);
    }

    /**
     * Conversas do usuário com alguém conectado agora.
     */
    public Set<UUID> watchedBy(UserId userId) {
        return this.owners.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(userId))
                .map(Map.Entry::getKey)
                .filter(id -> !this.listeners.getOrDefault(id, Set.of()).isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * A conversa foi apagada: avisa e desliga todos os ouvintes.
     */
    public void terminate(UUID conversationId) {
        Set<ConversationEventListener> removed = this.listeners.remove(conversationId);

        this.owners.remove(conversationId);

        if (isNull(removed)) {
            return;
        }

        removed.forEach(ConversationEventListener::onTerminated);
    }

    private void unsubscribe(
            UUID id,
            ConversationEventListener listener
    ) {
        Set<ConversationEventListener> current = this.listeners.get(id);

        if (isNull(current)) {
            return;
        }

        current.remove(listener);
    }

    private Object lockFor(UUID id) {
        return this.locks.computeIfAbsent(id, key -> new Object());
    }

    /**
     * Um ouvinte com problema (WebSocket fechado) não pode parar o turno nem os outros ouvintes.
     */
    private void deliver(
            ConversationEventListener listener,
            AssistantEvent event
    ) {
        try {
            listener.onEvent(event);
        } catch (RuntimeException e) {
            log.debug("Ouvinte da conversa {} falhou no evento {}: {}", event.conversationId(), event.seq(), e.getMessage());
        }
    }
}
