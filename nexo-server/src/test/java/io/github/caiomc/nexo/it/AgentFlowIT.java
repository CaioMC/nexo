package io.github.caiomc.nexo.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.it.support.FakeUpstream;
import io.github.caiomc.nexo.it.support.TestLoginConfig;
import io.github.caiomc.nexo.it.support.TestSocket;
import io.github.caiomc.nexo.support.TestUsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O agente de ponta a ponta (M3 a M5): WebSocket com sessão de verdade, laço com tool search no
 * cliente (RAG + DAG no banco real), trava do DAG, contexto desligado, aprovação, Parar e replay.
 * O modelo, o endpoint de token e as APIs do Google são um servidor HTTP local roteirizado.
 */
@AutoConfigureMockMvc
@Import(TestLoginConfig.class)
class AgentFlowIT extends PostgresIT {

    private static final String ORIGIN = "http://localhost:5173";
    private static final FakeUpstream UPSTREAM;

    static {
        try {
            UPSTREAM = new FakeUpstream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void upstream(DynamicPropertyRegistry registry) {
        registry.add("nexo.openai.base-url", () -> UPSTREAM.baseUrl() + "/v1");
        registry.add("nexo.openai.api-key", () -> "chave-de-teste");
        registry.add("nexo.retrieval.strategy", () -> "FULL_TEXT");
        registry.add("nexo.identity.google-token-uri", () -> UPSTREAM.baseUrl() + "/token");
        registry.add("nexo.identity.google-revoke-uri", () -> UPSTREAM.baseUrl() + "/revoke");
        registry.add("nexo.agent.google-api-base", () -> UPSTREAM.baseUrl() + "/google");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TokenCipherPort cipher;

    @Autowired
    private ObjectMapper json;

    private final List<WebSocketSession> sockets = new ArrayList<>();

    private SessionUser user;
    private String cookie;

    @BeforeEach
    void signIn() {
        this.user = newUser(this.jdbc, "agente@exemplo.com");
        enable(this.jdbc, this.user, "calendar");
        this.credential(this.user);
        this.cookie = this.login(this.user);

        UPSTREAM.google("GET /google/calendar/users/me/calendarList", "{\"items\":[{\"id\":\"primary\",\"summary\":\"Agenda da Ana\",\"primary\":true}]}");
        UPSTREAM.google("GET /google/calendar/calendars/primary/events", "{\"items\":[]}");
        UPSTREAM.google("POST /google/calendar/calendars/primary/events", "{\"id\":\"ev1\",\"summary\":\"Reunião com a Ana\"}");
        UPSTREAM.googleRequests.clear();
    }

    @AfterEach
    void closeSockets() throws IOException {
        for (WebSocketSession socket : this.sockets) {
            if (socket.isOpen()) {
                socket.close();
            }
        }
    }

    @Test
    void toolSearchDagAndApprovalUntilTheFinalAnswer() throws Exception {
        UPSTREAM.script(
                FakeUpstream.toolSearch("ts1", "criar evento de reunião na agenda"),
                FakeUpstream.functionCall("c1", "google_calendar_list_calendars", "{}"),
                FakeUpstream.functionCall("c2", "google_calendar_list_events", "{\"timeMin\":\"2026-10-14T00:00:00-03:00\",\"timeMax\":\"2026-10-15T00:00:00-03:00\",\"query\":null,\"maxResults\":10}"),
                FakeUpstream.functionCall("c3", "google_calendar_create_event", "{\"summary\":\"Reunião com a Ana\",\"start\":\"2026-10-14T10:00:00-03:00\",\"end\":\"2026-10-14T11:00:00-03:00\",\"calendarId\":\"primary\",\"description\":null,\"attendees\":[\"ana@exemplo.com\"]}"),
                FakeUpstream.finalText("Pronto, reunião criada para amanhã às 10h.")
        );

        UUID conversation = this.newConversation();
        TestSocket socket = this.connect(conversation, 0);
        String messageId = UUID.randomUUID().toString();

        socket.send("{\"kind\":\"command\",\"id\":\"" + messageId + "\",\"type\":\"user.message\",\"payload\":{\"text\":\"marque reunião amanhã 10h com a Ana\",\"timeZone\":\"America/Sao_Paulo\"}}");

        JsonNode ack = socket.await("ack");
        JsonNode approval = socket.awaitEvent("approval.requested");

        assertThat(ack.path("commandId").asText()).isEqualTo(messageId);
        assertThat(approval.path("payload").path("severity").asText()).isEqualTo("warning");
        assertThat(approval.path("payload").path("description").asText()).contains("Reunião com a Ana").contains("ana@exemplo.com");

        this.mvc.perform(get("/api/conversations/" + conversation + "/approvals").param("status", "PENDING").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        socket.send("{\"kind\":\"command\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"approval.decide\",\"payload\":{\"approvalId\":\"" + approval.path("payload").path("approvalId").asText() + "\",\"decision\":\"approve\"}}");

        JsonNode completed = socket.awaitEvent("turn.completed");

        assertThat(socket.eventTypes()).containsSubsequence(
                "message.user",
                "turn.started",
                "tool.search",
                "tool.started",
                "tool.completed",
                "tool.started",
                "tool.completed",
                "approval.requested",
                "approval.resolved",
                "tool.started",
                "tool.completed",
                "assistant.delta",
                "usage.updated",
                "assistant.message",
                "turn.completed"
        );
        assertThat(socket.eventOf("tool.search").path("payload").path("tools").toString()).contains("google_calendar_create_event").contains("\"origin\":\"DAG\"");
        assertThat(socket.eventOf("assistant.message").path("payload").path("text").asText()).isEqualTo("Pronto, reunião criada para amanhã às 10h.");
        assertThat(completed.path("payload").path("toolCalls").asInt()).isEqualTo(3);
        assertThat(completed.path("payload").path("usage").path("modelCalls").asInt()).isEqualTo(5);

        assertThat(UPSTREAM.modelRequests.getFirst()).contains("\"tool_search\"").contains("google_calendar_event").contains("Contextos liberados pelo usuário: calendar");
        assertThat(UPSTREAM.modelRequests.get(1)).contains("\"tool_search_output\"").contains("google_calendar_create_event").contains("defer_loading");
        assertThat(UPSTREAM.googleRequests).anyMatch(request -> request.startsWith("POST /google/calendar/calendars/primary/events") && request.contains("Reunião com a Ana"));

        assertThat(this.jdbc.sql("SELECT a.status FROM tool_approval a JOIN turn t ON t.id = a.turn_id WHERE t.conversation_id = :c").param("c", conversation).query(String.class).single()).isEqualTo("CONSUMED");
        assertThat(this.jdbc.sql("SELECT count(*) FROM tool_call c JOIN turn t ON t.id = c.turn_id WHERE t.conversation_id = :c AND c.status = 'SUCCEEDED'").param("c", conversation).query(Long.class).single()).isEqualTo(3);
        assertThat(this.jdbc.sql("SELECT title FROM conversation WHERE id = :c").param("c", conversation).query(String.class).single()).isEqualTo("marque reunião amanhã 10h com a Ana");
    }

    @Test
    void dagLockAndDisabledContextBlockTheCallsAndTheMessageIsIdempotent() throws Exception {
        UPSTREAM.script(
                FakeUpstream.functionCall("b1", "google_calendar_create_event", "{\"summary\":\"X\",\"start\":\"2026-10-14T10:00:00-03:00\",\"end\":\"2026-10-14T11:00:00-03:00\",\"calendarId\":\"primary\",\"description\":null,\"attendees\":[]}"),
                FakeUpstream.functionCall("b2", "google_gmail_search", "{\"query\":\"fatura\",\"maxResults\":5}"),
                FakeUpstream.finalText("Não consegui fazer isso agora.")
        );

        UUID conversation = this.newConversation();
        TestSocket socket = this.connect(conversation, 0);
        String messageId = UUID.randomUUID().toString();
        String message = "{\"kind\":\"command\",\"id\":\"" + messageId + "\",\"type\":\"user.message\",\"payload\":{\"text\":\"crie o evento X e procure a fatura\"}}";

        socket.send(message);

        JsonNode ack = socket.await("ack");

        socket.awaitEvent("turn.completed");

        List<JsonNode> blocked = socket.events("tool.blocked");

        assertThat(blocked).hasSize(2);
        assertThat(blocked.get(0).path("payload").path("reason").asText()).isEqualTo("MISSING_PREREQUISITES");
        assertThat(blocked.get(0).path("payload").path("missing").toString()).contains("google_calendar_list_events");
        assertThat(blocked.get(1).path("payload").path("reason").asText()).isEqualTo("CONTEXT_DISABLED");
        assertThat(UPSTREAM.modelRequests.get(1)).contains("missing_prerequisites").contains("Execute antes: google_calendar_list_events");
        assertThat(UPSTREAM.modelRequests.get(2)).contains("context_disabled");
        assertThat(UPSTREAM.googleRequests).isEmpty();

        socket.send(message);

        assertThat(socket.await("ack").path("turnId").asText()).isEqualTo(ack.path("turnId").asText());
        assertThat(this.jdbc.sql("SELECT count(*) FROM turn WHERE conversation_id = :c").param("c", conversation).query(Long.class).single()).isEqualTo(1);

        long last = socket.eventOf("turn.completed").path("seq").asLong();
        TestSocket replay = this.connect(conversation, 3);
        JsonNode lastReplayed = replay.awaitSeq(last);

        assertThat(replay.seqs().getFirst()).isEqualTo(4);
        assertThat(replay.seqs()).isSortedAccordingTo(Long::compare).doesNotHaveDuplicates().hasSize((int) last - 3);
        assertThat(lastReplayed.path("type").asText()).isEqualTo("turn.completed");
    }

    @Test
    void destructiveCallWithoutItsPrerequisiteIsBlockedAndModelFailureFailsTheTurn() throws Exception {
        UPSTREAM.script(
                FakeUpstream.functionCall("s1", "google_calendar_list_calendars", "{}"),
                FakeUpstream.functionCall("s2", "google_calendar_list_events", "{\"timeMin\":null,\"timeMax\":null,\"query\":null,\"maxResults\":5}"),
                FakeUpstream.functionCall("s3", "google_calendar_delete_event", "{\"eventId\":\"ev9\",\"calendarId\":\"primary\",\"summary\":\"Daily\"}")
        );

        UUID conversation = this.newConversation();
        TestSocket socket = this.connect(conversation, 0);

        socket.send("{\"kind\":\"command\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"user.message\",\"payload\":{\"text\":\"apague a daily\"}}");
        socket.await("ack");

        JsonNode blockedOnPrerequisite = socket.awaitEvent("tool.blocked");

        assertThat(blockedOnPrerequisite.path("payload").path("missing").toString()).contains("google_calendar_get_event");
        assertThat(this.jdbc.sql("SELECT count(*) FROM tool_approval a JOIN turn t ON t.id = a.turn_id WHERE t.conversation_id = :c").param("c", conversation).query(Long.class).single()).isZero();

        socket.awaitEvent("turn.failed");

        assertThat(socket.eventOf("turn.failed").path("payload").path("code").asText()).isEqualTo("MODEL_ERROR");
    }

    @Test
    void stopDuringApprovalAndSecondMessageIsRefused() throws Exception {
        UPSTREAM.script(
                FakeUpstream.functionCall("p1", "google_calendar_list_calendars", "{}"),
                FakeUpstream.functionCall("p2", "google_calendar_list_events", "{\"timeMin\":null,\"timeMax\":null,\"query\":null,\"maxResults\":5}"),
                FakeUpstream.functionCall("p3", "google_calendar_create_event", "{\"summary\":\"Y\",\"start\":\"2026-10-14T10:00:00-03:00\",\"end\":\"2026-10-14T11:00:00-03:00\",\"calendarId\":\"primary\",\"description\":null,\"attendees\":[]}")
        );

        UUID conversation = this.newConversation();
        TestSocket socket = this.connect(conversation, 0);

        socket.send("{\"kind\":\"command\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"user.message\",\"payload\":{\"text\":\"crie o evento Y\"}}");
        socket.await("ack");
        socket.awaitEvent("approval.requested");

        String secondId = UUID.randomUUID().toString();

        socket.send("{\"kind\":\"command\",\"id\":\"" + secondId + "\",\"type\":\"user.message\",\"payload\":{\"text\":\"outra coisa\"}}");

        JsonNode nack = socket.await("nack");

        assertThat(nack.path("commandId").asText()).isEqualTo(secondId);
        assertThat(nack.path("code").asText()).isEqualTo("TURN_IN_PROGRESS");

        socket.send("{\"kind\":\"command\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"turn.stop\",\"payload\":{}}");

        JsonNode failed = socket.awaitEvent("turn.failed");

        assertThat(failed.path("payload").path("code").asText()).isEqualTo("STOPPED");
        assertThat(socket.eventOf("approval.resolved").path("payload").path("status").asText()).isEqualTo("REJECTED");
        assertThat(this.jdbc.sql("SELECT a.status FROM tool_approval a JOIN turn t ON t.id = a.turn_id WHERE t.conversation_id = :c").param("c", conversation).query(String.class).single()).isEqualTo("REJECTED");
        assertThat(UPSTREAM.googleRequests).noneMatch(request -> request.startsWith("POST"));
    }

    @Test
    void contextsAreListedEnabledAndDisabledAndOpenConversationsHearIt() throws Exception {
        UUID conversation = this.newConversation();
        TestSocket socket = this.connect(conversation, 0);

        this.mvc.perform(get("/api/contexts").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[?(@.key == 'calendar')].enabled").value(true))
                .andExpect(jsonPath("$[?(@.key == 'calendar')].scopesGranted").value(true))
                .andExpect(jsonPath("$[?(@.key == 'calendar')].toolCount").value(7))
                .andExpect(jsonPath("$[?(@.key == 'gmail')].scopesGranted").value(false));

        this.mvc.perform(post("/api/contexts/gmail/enable").with(TestUsers.as(this.user)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("consent_required"))
                .andExpect(jsonPath("$.authorizationUrl").value("/oauth2/authorization/google?context=gmail"));

        this.mvc.perform(post("/api/contexts/calendar/disable").with(TestUsers.as(this.user)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        JsonNode changed = socket.awaitEvent("context.changed");

        assertThat(changed.path("payload").path("context").asText()).isEqualTo("calendar");
        assertThat(changed.path("payload").path("enabled").asBoolean()).isFalse();

        this.mvc.perform(post("/api/contexts/calendar/enable").with(TestUsers.as(this.user)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("enabled"));

        this.mvc.perform(post("/api/contexts/fax/enable").with(TestUsers.as(this.user)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://nexo.dev/problems/contexto-nao-encontrado"));
    }

    @Test
    void meLogoutAndDisconnect() throws Exception {
        this.mvc.perform(get("/api/me").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("agente@exemplo.com"));

        this.mvc.perform(post("/api/auth/logout").with(TestUsers.as(this.user)).with(csrf())).andExpect(status().isNoContent());
        assertThat(this.jdbc.sql("SELECT count(*) FROM google_credential WHERE user_id = :u").param("u", this.user.userId()).query(Long.class).single()).isEqualTo(1);

        this.mvc.perform(post("/api/auth/disconnect").with(TestUsers.as(this.user)).with(csrf())).andExpect(status().isNoContent());

        assertThat(this.jdbc.sql("SELECT count(*) FROM google_credential WHERE user_id = :u").param("u", this.user.userId()).query(Long.class).single()).isZero();
        assertThat(this.jdbc.sql("SELECT count(*) FROM user_context WHERE user_id = :u AND enabled").param("u", this.user.userId()).query(Long.class).single()).isZero();
        assertThat(this.jdbc.sql("SELECT count(*) FROM user_context WHERE user_id = :u").param("u", this.user.userId()).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void stateChangingRequestsWithoutCsrfAreRefused() throws Exception {
        this.mvc.perform(post("/api/conversations").with(TestUsers.as(this.user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://nexo.dev/problems/csrf-invalido"));
    }

    @Test
    void anotherUsersConversationClosesWith4404() throws Exception {
        UUID conversation = this.newConversation();
        SessionUser intruder = newUser(this.jdbc, "intruso@exemplo.com");
        TestSocket socket = this.connect(conversation, 0, this.login(intruder));

        assertThat(socket.awaitClose().getCode()).isEqualTo(4404);
    }

    private UUID newConversation() throws Exception {
        String body = this.mvc.perform(post("/api/conversations").with(TestUsers.as(this.user)).with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(this.json.readTree(body).path("id").asText());
    }

    private void credential(SessionUser owner) {
        EncryptedToken token = this.cipher.encrypt("refresh-de-teste", UserId.of(owner.userId()));

        this.jdbc.sql("INSERT INTO google_credential (user_id, refresh_token, key_version, granted_scopes, updated_at) VALUES (:user, :token, :version, ARRAY['https://www.googleapis.com/auth/calendar'], :now)")
                .param("user", owner.userId())
                .param("token", token.bytes())
                .param("version", token.keyVersion())
                .param("now", Timestamp.from(Instant.now()))
                .update();
    }

    /**
     * Sessão de verdade no Spring Session JDBC: o cookie que o navegador teria depois do login.
     */
    private String login(SessionUser owner) {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + this.port + "/test/login?userId=" + owner.userId() + "&email=" + owner.email()))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.headers().allValues("Set-Cookie").stream()
                    .filter(value -> value.startsWith("SESSION="))
                    .map(value -> value.substring(0, value.indexOf(';')))
                    .findFirst()
                    .orElseThrow();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Login de teste falhou", e);
        }
    }

    private TestSocket connect(
            UUID conversation,
            long lastSeq
    ) throws Exception {
        return this.connect(conversation, lastSeq, this.cookie);
    }

    private TestSocket connect(
            UUID conversation,
            long lastSeq,
            String sessionCookie
    ) throws Exception {
        TestSocket socket = new TestSocket(this.json);
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        headers.add("Cookie", sessionCookie);
        headers.setOrigin(ORIGIN);

        WebSocketSession session = new StandardWebSocketClient()
                .execute(socket, headers, URI.create("ws://127.0.0.1:" + this.port + "/ws/conversations/" + conversation + "?lastSeq=" + lastSeq))
                .get(5, TimeUnit.SECONDS);

        socket.attach(session);
        this.sockets.add(session);

        return socket;
    }
}
