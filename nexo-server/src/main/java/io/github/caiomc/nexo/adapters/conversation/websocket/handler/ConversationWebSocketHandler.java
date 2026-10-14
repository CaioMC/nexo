package io.github.caiomc.nexo.adapters.conversation.websocket.handler;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.conversation.websocket.mapper.EventMessageMapper;
import io.github.caiomc.nexo.adapters.conversation.websocket.message.ClientCommandMessage;
import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.conversation.domain.exception.ApprovalExpiredException;
import io.github.caiomc.nexo.core.conversation.domain.exception.ApprovalNotFoundException;
import io.github.caiomc.nexo.core.conversation.domain.exception.ApprovalNotPendingException;
import io.github.caiomc.nexo.core.conversation.domain.exception.ConversationNotFoundException;
import io.github.caiomc.nexo.core.conversation.domain.exception.ModelNotConfiguredException;
import io.github.caiomc.nexo.core.conversation.domain.exception.NoActiveTurnException;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnInProgressException;
import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.usecase.ConversationEventListener;
import io.github.caiomc.nexo.core.conversation.usecase.ConversationWatch;
import io.github.caiomc.nexo.core.conversation.usecase.DecideApprovalUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.SendMessageUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.StopTurnUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.WatchConversationUseCase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * {@code WS /ws/conversations/{id}?lastSeq=N} (doc 06): envia os eventos com {@code seq > N} já
 * gravados e depois os novos, ao vivo; recebe {@code user.message}, {@code approval.decide} e
 * {@code turn.stop}. Traduz (mapper) e delega (casos de uso).
 */
public class ConversationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConversationWebSocketHandler.class);

    public static final CloseStatus BAD_REQUEST = new CloseStatus(4400, "lastSeq ou caminho inválido");
    public static final CloseStatus UNAUTHORIZED = new CloseStatus(4401, "sessão expirou");
    public static final CloseStatus NOT_FOUND = new CloseStatus(4404, "conversa não encontrada");
    public static final CloseStatus DELETED = new CloseStatus(4409, "conversa apagada");

    private static final String WATCH = "nexo.watch";
    private static final String CONVERSATION = "nexo.conversation";
    private static final String SENDER = "nexo.sender";
    private static final String LAST_PONG = "nexo.lastPong";

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_LIMIT_BYTES = 4 * 1024 * 1024;
    private static final Duration PONG_TIMEOUT = Duration.ofSeconds(60);
    private static final Set<String> COMMANDS = Set.of(ClientCommandMessage.USER_MESSAGE, ClientCommandMessage.APPROVAL_DECIDE, ClientCommandMessage.TURN_STOP);

    private final WatchConversationUseCase watch;
    private final SendMessageUseCase send;
    private final DecideApprovalUseCase decide;
    private final StopTurnUseCase stop;

    private final EventMessageMapper mapper;
    private final ObjectMapper json;

    private final Set<WebSocketSession> open = ConcurrentHashMap.newKeySet();

    public ConversationWebSocketHandler(
            WatchConversationUseCase watch,
            SendMessageUseCase send,
            DecideApprovalUseCase decide,
            StopTurnUseCase stop,
            EventMessageMapper mapper,
            ObjectMapper json
    ) {
        this.watch = watch;
        this.send = send;
        this.decide = decide;
        this.stop = stop;
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        SessionUser user = this.user(session);

        if (isNull(user)) {
            session.close(UNAUTHORIZED);

            return;
        }

        UUID conversationId;
        long lastSeq;

        try {
            conversationId = this.conversationId(session.getUri());
            lastSeq = this.lastSeq(session.getUri());
        } catch (IllegalArgumentException e) {
            session.close(BAD_REQUEST);

            return;
        }

        WebSocketSession sender = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES);

        session.getAttributes().put(SENDER, sender);
        session.getAttributes().put(CONVERSATION, conversationId);
        session.getAttributes().put(LAST_PONG, System.nanoTime());

        try {
            ConversationWatch subscription = this.watch.watch(
                    this.mapper.toWatchCommand(user, conversationId, lastSeq),
                    this.listener(sender)
            );

            session.getAttributes().put(WATCH, subscription);
            this.open.add(session);
        } catch (ConversationNotFoundException e) {
            session.close(NOT_FOUND);
        }
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {
        WebSocketSession sender = (WebSocketSession) session.getAttributes().get(SENDER);
        SessionUser user = this.user(session);
        UUID conversationId = (UUID) session.getAttributes().get(CONVERSATION);
        ClientCommandMessage command;

        try {
            command = this.json.readValue(message.getPayload(), ClientCommandMessage.class);
        } catch (JsonProcessingException e) {
            this.write(sender, this.mapper.toNack(null, "INVALID_PAYLOAD", "JSON inválido"));

            return;
        }

        if (isNull(command.type()) || !COMMANDS.contains(command.type())) {
            this.write(sender, this.mapper.toNack(command.id(), "UNKNOWN_COMMAND", "Comando desconhecido: " + command.type()));

            return;
        }

        try {
            this.write(sender, this.mapper.toAck(command.id(), this.dispatch(user, conversationId, command)));
        } catch (TurnInProgressException e) {
            this.write(sender, this.mapper.toNack(command.id(), "TURN_IN_PROGRESS", e.getMessage()));
        } catch (NoActiveTurnException e) {
            this.write(sender, this.mapper.toNack(command.id(), "NO_ACTIVE_TURN", e.getMessage()));
        } catch (ApprovalExpiredException e) {
            this.write(sender, this.mapper.toNack(command.id(), "APPROVAL_EXPIRED", e.getMessage()));
        } catch (ApprovalNotPendingException e) {
            this.write(sender, this.mapper.toNack(command.id(), "APPROVAL_NOT_PENDING", e.getMessage()));
        } catch (ApprovalNotFoundException | ConversationNotFoundException e) {
            this.write(sender, this.mapper.toNack(command.id(), "NOT_FOUND", e.getMessage()));
        } catch (ModelNotConfiguredException e) {
            this.write(sender, this.mapper.toNack(command.id(), "MODEL_NOT_CONFIGURED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            this.write(sender, this.mapper.toNack(command.id(), "INVALID_PAYLOAD", e.getMessage()));
        }
    }

    @Override
    protected void handlePongMessage(
            WebSocketSession session,
            PongMessage message
    ) {
        session.getAttributes().put(LAST_PONG, System.nanoTime());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        this.open.remove(session);

        if (session.getAttributes().get(WATCH) instanceof ConversationWatch subscription) {
            subscription.close();
        }
    }

    /**
     * Ping a cada 25 s; quem não responde há 60 s é fechado.
     */
    @Scheduled(fixedRate = 25_000, initialDelay = 25_000)
    public void keepAlive() {
        for (WebSocketSession session : this.open) {
            long lastPong = (long) session.getAttributes().getOrDefault(LAST_PONG, System.nanoTime());
            WebSocketSession sender = (WebSocketSession) session.getAttributes().get(SENDER);

            try {
                if (Duration.ofNanos(System.nanoTime() - lastPong).compareTo(PONG_TIMEOUT) > 0) {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } else if (nonNull(sender)) {
                    sender.sendMessage(new PingMessage(ByteBuffer.allocate(0)));
                }
            } catch (IOException | IllegalStateException e) {
                log.debug("Keep-alive falhou para {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    /**
     * @return o turno do {@code ack} ({@code user.message}); nulo nos outros comandos
     */
    private UUID dispatch(
            SessionUser user,
            UUID conversationId,
            ClientCommandMessage command
    ) {
        switch (command.type()) {
            case ClientCommandMessage.USER_MESSAGE -> {
                Turn turn = this.send.send(this.mapper.toSendCommand(user, conversationId, command));

                return turn.id();
            }
            case ClientCommandMessage.APPROVAL_DECIDE -> this.decide.decide(this.mapper.toDecideCommand(user, conversationId, command));
            case ClientCommandMessage.TURN_STOP -> this.stop.stop(this.mapper.toStopCommand(user, conversationId));
            default -> throw new IllegalArgumentException("Comando desconhecido: " + command.type());
        }

        return null;
    }

    private ConversationEventListener listener(WebSocketSession sender) {
        return new ConversationEventListener() {
            @Override
            public void onEvent(AssistantEvent event) {
                ConversationWebSocketHandler.this.write(sender, ConversationWebSocketHandler.this.mapper.toMessage(event));
            }

            @Override
            public void onTerminated() {
                ConversationWebSocketHandler.this.close(sender, DELETED);
            }
        };
    }

    private void write(
            WebSocketSession sender,
            Object message
    ) {
        if (!sender.isOpen()) {
            return;
        }

        try {
            sender.sendMessage(new TextMessage(this.json.writeValueAsString(message)));
        } catch (JsonProcessingException e) {
            log.warn("Mensagem do WebSocket não serializável: {}", e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException("Falha enviando pelo WebSocket", e);
        }
    }

    private void close(
            WebSocketSession sender,
            CloseStatus status
    ) {
        try {
            sender.close(status);
        } catch (IOException e) {
            log.debug("Falha fechando WebSocket: {}", e.getMessage());
        }
    }

    private SessionUser user(WebSocketSession session) {
        return session.getAttributes().get(SessionHandshakeInterceptor.USER) instanceof SessionUser user ? user : null;
    }

    private UUID conversationId(URI uri) {
        String path = uri.getPath();

        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    private long lastSeq(URI uri) {
        Map<String, List<String>> query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String value = isNull(query.get("lastSeq")) ? null : query.get("lastSeq").getFirst();

        if (isNull(value)) {
            return 0;
        }

        try {
            long seq = Long.parseLong(value);

            if (seq < 0) {
                throw new IllegalArgumentException("lastSeq negativo");
            }

            return seq;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("lastSeq inválido: " + value);
        }
    }
}
