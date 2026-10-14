package io.github.caiomc.nexo.core.conversation.application;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.agent.TurnConductorService;
import io.github.caiomc.nexo.core.conversation.domain.exception.ModelNotConfiguredException;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnInProgressException;
import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.mapper.AgentCommandMapper;
import io.github.caiomc.nexo.core.conversation.mapper.ConversationDataMapper;
import io.github.caiomc.nexo.core.conversation.mapper.TurnDataMapper;
import io.github.caiomc.nexo.core.conversation.port.model.LanguageModelPort;
import io.github.caiomc.nexo.core.conversation.port.repository.ConversationRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.conversation.usecase.SendMessageUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.command.SendMessageCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;

import static java.util.Objects.isNull;

/**
 * {@code user.message}: grava a mensagem, cria o turno (um por vez, RF-25; idempotente pelo
 * {@code clientMessageId}, RF-26) e entrega ao {@link TurnConductorService}.
 */
@Service
public class SendMessageService implements SendMessageUseCase {

    private final LanguageModelPort model;
    private final ConversationQueryService queries;
    private final ConversationRepository conversations;
    private final TurnRepository turns;
    private final GetEnabledContextsUseCase contexts;
    private final ConversationEventHubService hub;
    private final TurnConductorService conductor;

    private final ConversationEvents events;
    private final TurnDataMapper turnMapper;
    private final ConversationDataMapper conversationMapper;
    private final AgentCommandMapper commands;

    private final AgentSettings settings;
    private final Clock clock;

    public SendMessageService(
            LanguageModelPort model,
            ConversationQueryService queries,
            ConversationRepository conversations,
            TurnRepository turns,
            GetEnabledContextsUseCase contexts,
            ConversationEventHubService hub,
            TurnConductorService conductor,
            ConversationEvents events,
            TurnDataMapper turnMapper,
            ConversationDataMapper conversationMapper,
            AgentCommandMapper commands,
            AgentSettings settings,
            Clock clock
    ) {
        this.model = model;
        this.queries = queries;
        this.conversations = conversations;
        this.turns = turns;
        this.contexts = contexts;
        this.hub = hub;
        this.conductor = conductor;
        this.events = events;
        this.turnMapper = turnMapper;
        this.conversationMapper = conversationMapper;
        this.commands = commands;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public Turn send(SendMessageCommand command) {
        Conversation conversation = this.queries.owned(command.userId(), command.conversationId());
        Optional<Turn> repeated = this.turns.findByClientMessage(conversation.id(), command.clientMessageId()).map(this.turnMapper::toTurn);

        if (repeated.isPresent()) {
            return repeated.get();
        }

        if (!this.model.isConfigured()) {
            throw new ModelNotConfiguredException();
        }

        if (this.turns.findActive(conversation.id()).isPresent()) {
            throw new TurnInProgressException(conversation.id());
        }

        Instant now = this.clock.instant();
        Set<ContextKey> enabled = this.contexts.enabled(this.commands.toEnabledContexts(command.userId()));
        Turn turn = this.turnMapper.start(
                command,
                this.model.modelName(),
                enabled,
                now
        );

        this.turns.insert(this.turnMapper.toData(turn));
        this.hub.publish(conversation.id(), this.events.messageUser(turn.id(), command.text(), command.clientMessageId()));
        this.touchConversation(conversation, command.text(), now);
        this.conductor.start(turn, this.userZone(command.timeZone()));

        return turn;
    }

    /**
     * Título automático no 1º turno; nos outros, só sobe a conversa na lista.
     */
    private void touchConversation(
            Conversation conversation,
            String text,
            Instant now
    ) {
        Conversation updated = conversation.renamed(conversation.hasDefaultTitle() ? Conversation.titleFrom(text) : conversation.title(), now);

        this.conversations.save(this.conversationMapper.toData(updated));

        if (conversation.hasDefaultTitle()) {
            this.hub.publish(conversation.id(), this.events.conversationRenamed(updated.title()));
        }
    }

    /**
     * O fuso do navegador; sem ele (ou inválido), o padrão do agente.
     */
    private ZoneId userZone(String timeZone) {
        if (isNull(timeZone) || timeZone.isBlank()) {
            return this.settings.defaultTimeZone();
        }

        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            return this.settings.defaultTimeZone();
        }
    }
}
