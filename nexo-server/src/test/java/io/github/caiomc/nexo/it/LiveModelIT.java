package io.github.caiomc.nexo.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O laço com o modelo de verdade (o proxy do {@code .env}), só com {@code NEXO_LIVE=1}: confere que o
 * provedor aceita {@code tool_search} no cliente, {@code tool_search_output} com
 * {@code defer_loading} e {@code function_call_output}. O Google continua falso.
 */
@AutoConfigureMockMvc
@Import(TestLoginConfig.class)
class LiveModelIT extends PostgresIT {

    private static final Map<String, String> ENV = dotenv();
    private static final FakeUpstream GOOGLE;

    static {
        try {
            GOOGLE = new FakeUpstream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void live(DynamicPropertyRegistry registry) {
        registry.add("nexo.openai.base-url", () -> ENV.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"));
        registry.add("nexo.openai.api-key", () -> ENV.getOrDefault("OPENAI_API_KEY", ""));
        registry.add("nexo.openai.model", () -> ENV.getOrDefault("OPENAI_MODEL", "gpt-5.4-mini"));
        registry.add("nexo.retrieval.strategy", () -> "FULL_TEXT");
        registry.add("nexo.identity.google-token-uri", () -> GOOGLE.baseUrl() + "/token");
        registry.add("nexo.agent.google-api-base", () -> GOOGLE.baseUrl() + "/google");
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

    private SessionUser user;

    @BeforeEach
    void onlyWhenAsked() {
        assumeTrue("1".equals(System.getenv("NEXO_LIVE")) && !ENV.getOrDefault("OPENAI_API_KEY", "").isBlank(), "NEXO_LIVE=1 e OPENAI_API_KEY no .env");

        this.user = newUser(this.jdbc, "ao-vivo@exemplo.com");
        enable(this.jdbc, this.user, "calendar");

        EncryptedToken token = this.cipher.encrypt("refresh", UserId.of(this.user.userId()));

        this.jdbc.sql("INSERT INTO google_credential (user_id, refresh_token, key_version, granted_scopes, updated_at) VALUES (:u, :t, :v, ARRAY['https://www.googleapis.com/auth/calendar'], :now)")
                .param("u", this.user.userId())
                .param("t", token.bytes())
                .param("v", token.keyVersion())
                .param("now", Timestamp.from(Instant.now()))
                .update();

        GOOGLE.google("GET /google/calendar/users/me/calendarList", "{\"items\":[{\"id\":\"primary\",\"summary\":\"Agenda\",\"primary\":true,\"timeZone\":\"America/Sao_Paulo\"}]}");
        GOOGLE.google("GET /google/calendar/calendars/primary/events", "{\"items\":[{\"id\":\"ev1\",\"summary\":\"Daily do time\",\"start\":{\"dateTime\":\"2026-10-14T09:30:00-03:00\"},\"end\":{\"dateTime\":\"2026-10-14T09:45:00-03:00\"}}]}");
        GOOGLE.google("POST /google/calendar/calendars/primary/events", "{\"id\":\"ev2\",\"summary\":\"Alinhamento\",\"status\":\"confirmed\"}");
    }

    @Test
    void realModelReadsTheAgendaThroughToolSearchAndCreatesAnEventWithApproval() throws Exception {
        TestSocket socket = this.connect(this.newConversation());

        this.say(socket, "O que tenho na agenda amanhã?");

        JsonNode first = socket.awaitEvent("turn.completed", 120);

        assertThat(socket.eventTypes()).contains("tool.search");
        assertThat(socket.events("tool.completed")).anyMatch(event -> "google_calendar_list_events".equals(event.path("payload").path("tool").asText()));
        assertThat(socket.eventOf("assistant.message").path("payload").path("text").asText()).containsIgnoringCase("daily");
        System.out.println("[ao vivo] turno 1: " + first.path("payload") + " → " + socket.eventTypes());

        this.say(socket, "Crie amanhã às 15h um evento chamado Alinhamento, de 30 minutos, com ana@exemplo.com.");

        JsonNode approval = socket.awaitEvent("approval.requested", 120);

        System.out.println("[ao vivo] aprovação: " + approval.path("payload").path("description").asText());
        assertThat(approval.path("payload").path("tool").asText()).isEqualTo("google_calendar_create_event");

        socket.send("{\"kind\":\"command\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"approval.decide\",\"payload\":{\"approvalId\":\"" + approval.path("payload").path("approvalId").asText() + "\",\"decision\":\"approve\"}}");

        JsonNode second = socket.awaitEvent("turn.completed", 120);

        System.out.println("[ao vivo] turno 2: " + second.path("payload") + " → " + socket.events("assistant.message").getLast().path("payload").path("text").asText());
        assertThat(GOOGLE.googleRequests).anyMatch(request -> request.startsWith("POST /google/calendar/calendars/primary/events") && request.contains("Alinhamento"));
    }

    private void say(
            TestSocket socket,
            String text
    ) throws IOException {
        socket.send("{\"kind\":\"command\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"user.message\",\"payload\":{\"text\":\"" + text + "\",\"timeZone\":\"America/Sao_Paulo\"}}");
    }

    private UUID newConversation() throws Exception {
        String body = this.mvc.perform(post("/api/conversations").with(TestUsers.as(this.user)).with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(this.json.readTree(body).path("id").asText());
    }

    private TestSocket connect(UUID conversation) throws Exception {
        HttpResponse<String> login = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + this.port + "/test/login?userId=" + this.user.userId() + "&email=" + this.user.email()))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        String cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
        TestSocket socket = new TestSocket(this.json);
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        headers.add("Cookie", cookie);
        headers.setOrigin("http://localhost:5173");
        socket.attach(new StandardWebSocketClient().execute(socket, headers, URI.create("ws://127.0.0.1:" + this.port + "/ws/conversations/" + conversation)).get(5, TimeUnit.SECONDS));

        return socket;
    }

    /**
     * Lê o {@code .env} da raiz sem expor nada: só o que o teste usa.
     */
    private static Map<String, String> dotenv() {
        Map<String, String> values = new HashMap<>();
        Path file = Path.of("..", ".env");

        if (!Files.exists(file)) {
            return values;
        }

        try {
            for (String line : Files.readAllLines(file)) {
                int equals = line.indexOf('=');

                if (line.isBlank() || line.startsWith("#") || equals < 0) {
                    continue;
                }

                values.put(line.substring(0, equals).strip(), line.substring(equals + 1).strip());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return values;
    }
}
