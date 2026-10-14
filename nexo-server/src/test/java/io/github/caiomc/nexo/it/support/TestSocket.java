package io.github.caiomc.nexo.it.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cliente WebSocket de teste: guarda tudo que chega e espera por tipo.
 */
public final class TestSocket extends TextWebSocketHandler {

    private final ObjectMapper json;
    private final BlockingQueue<JsonNode> inbox = new LinkedBlockingQueue<>();
    private final List<JsonNode> received = new ArrayList<>();
    private final BlockingQueue<CloseStatus> closed = new LinkedBlockingQueue<>();

    private WebSocketSession session;

    public TestSocket(ObjectMapper json) {
        this.json = json;
    }

    public void attach(WebSocketSession session) {
        this.session = session;
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws Exception {
        this.inbox.add(this.json.readTree(message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        this.closed.add(status);
    }

    public void send(String text) throws IOException {
        this.session.sendMessage(new TextMessage(text));
    }

    public JsonNode await(String kind) throws InterruptedException {
        return this.awaitMatching(node -> kind.equals(node.path("kind").asText()), kind, 15);
    }

    public JsonNode awaitEvent(String type) throws InterruptedException {
        return this.awaitEvent(type, 15);
    }

    public JsonNode awaitEvent(
            String type,
            int seconds
    ) throws InterruptedException {
        return this.awaitMatching(
                node -> "event".equals(node.path("kind").asText()) && type.equals(node.path("type").asText()),
                type,
                seconds
        );
    }

    public JsonNode awaitSeq(long seq) throws InterruptedException {
        return this.awaitMatching(node -> node.path("seq").asLong() == seq, "seq " + seq, 15);
    }

    public CloseStatus awaitClose() throws InterruptedException {
        CloseStatus status = this.closed.poll(10, TimeUnit.SECONDS);

        assertThat(status).as("o socket deveria ter fechado").isNotNull();

        return status;
    }

    public List<String> eventTypes() {
        return this.received.stream().filter(node -> "event".equals(node.path("kind").asText())).map(node -> node.path("type").asText()).toList();
    }

    public List<Long> seqs() {
        return this.received.stream().filter(node -> "event".equals(node.path("kind").asText())).map(node -> node.path("seq").asLong()).toList();
    }

    public List<JsonNode> events(String type) {
        return this.received.stream().filter(node -> type.equals(node.path("type").asText())).toList();
    }

    public JsonNode eventOf(String type) {
        return this.events(type).getFirst();
    }

    private JsonNode awaitMatching(
            Predicate<JsonNode> matches,
            String what,
            int seconds
    ) throws InterruptedException {
        for (JsonNode seen : this.received) {
            if (matches.test(seen) && !seen.path("consumed").asBoolean()) {
                ((ObjectNode) seen).put("consumed", true);

                return seen;
            }
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);

        while (System.nanoTime() < deadline) {
            JsonNode next = this.inbox.poll(200, TimeUnit.MILLISECONDS);

            if (isNull(next)) {
                continue;
            }

            this.received.add(next);

            if (matches.test(next)) {
                ((ObjectNode) next).put("consumed", true);

                return next;
            }
        }

        throw new AssertionError("Não chegou " + what + ". Recebido: " + this.received);
    }
}
