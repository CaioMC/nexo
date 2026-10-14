package io.github.caiomc.nexo.adapters.conversation.google;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.caiomc.nexo.adapters.conversation.google.executor.CalendarExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.DriveExecutor;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolOutcome;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolResultData;
import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.domain.model.GrantedScopes;
import io.github.caiomc.nexo.core.identity.usecase.ObtainAccessTokenUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.ObtainAccessTokenCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedCatalog;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O executor contra um servidor HTTP local no lugar do Google: 401 renova o token, 403 de escopo
 * vira {@code PERMISSION_DENIED}, refresh recusado vira {@code RECONNECT_REQUIRED}, saída grande é cortada.
 */
class GoogleToolExecutorAdapterTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private final Tokens tokens = new Tokens();

    private HttpServer server;
    private GoogleToolExecutorAdapter adapter;

    @BeforeEach
    void start() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/calendar/users/me/calendarList", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");

            this.authorizations.add(authorization);
            this.reply(exchange, "Bearer token-1".equals(authorization) ? 401 : 200, "Bearer token-1".equals(authorization) ? "{\"error\":{\"code\":401,\"message\":\"expirado\"}}" : "{\"items\":[{\"id\":\"primary\"}]}");
        });
        this.server.createContext("/drive/files", exchange -> this.reply(exchange, 403, "{\"error\":{\"code\":403,\"message\":\"Request had insufficient authentication scopes.\",\"errors\":[{\"reason\":\"insufficientPermissions\"}]}}"));
        this.server.createContext("/calendar/calendars/primary/events", exchange -> this.reply(exchange, 200, "{\"items\":[{\"summary\":\"" + "x".repeat(20_000) + "\"}]}"));
        this.server.start();

        this.adapter = new GoogleToolExecutorAdapter(
                List.of(new CalendarExecutor(), new DriveExecutor()),
                this.tokens,
                new FixedCatalog(CatalogFixtures.catalog()),
                new GoogleApiClient(this.json),
                GoogleApiEndpoints.under("http://127.0.0.1:" + this.server.getAddress().getPort()),
                new ResultSummarizer(),
                this.json
        );
    }

    @AfterEach
    void stop() {
        this.server.stop(0);
    }

    @Test
    void unauthorizedRefreshesTheTokenOnceAndSucceeds() {
        ToolResultData result = this.adapter.execute(this.command("calendar_list_calendars", "calendar", Map.of()));

        assertThat(result.outcome()).isEqualTo(ToolOutcome.SUCCEEDED);
        assertThat(result.summary()).isEqualTo("1 itens");
        assertThat(this.authorizations).containsExactly("Bearer token-1", "Bearer token-2");
        assertThat(this.tokens.invalidated).isEqualTo(1);
    }

    @Test
    void missingScopeIsPermissionDenied() {
        ToolResultData result = this.adapter.execute(this.command("drive_get_metadata", "drive", Map.of("fileId", "abc")));

        assertThat(result.outcome()).isEqualTo(ToolOutcome.PERMISSION_DENIED);
        assertThat(result.error()).contains("403");
    }

    @Test
    void revokedRefreshTokenAsksToReconnect() {
        this.tokens.revoked = true;

        assertThat(this.adapter.execute(this.command("calendar_list_calendars", "calendar", Map.of())).outcome()).isEqualTo(ToolOutcome.RECONNECT_REQUIRED);
    }

    @Test
    void outputLargerThanTheLimitIsTruncated() {
        this.tokens.issued = 1;

        ToolResultData result = this.adapter.execute(this.command("calendar_list_events", "calendar", Map.of("maxResults", 5)));

        assertThat(result.truncated()).isTrue();
        assertThat(result.output()).hasSize(16_000);
    }

    private ExecuteToolCommand command(
            String tool,
            String context,
            Map<String, Object> arguments
    ) {
        return new ExecuteToolCommand(
                UserId.of(UUID.randomUUID()),
                ToolName.of(this.googleName(tool)),
                ContextKey.of(context),
                arguments
        );
    }

    /**
     * O catálogo de teste usa nomes curtos; os executores, os do Google.
     */
    private String googleName(String tool) {
        return "google_" + tool;
    }

    private void reply(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final class Tokens implements ObtainAccessTokenUseCase {

        private int issued;
        private int invalidated;
        private boolean revoked;

        @Override
        public AccessToken obtain(ObtainAccessTokenCommand command) {
            if (this.revoked) {
                throw new ConsentRevokedException(command.userId());
            }

            this.issued++;

            return new AccessToken("token-" + this.issued, Instant.now().plusSeconds(3600), GrantedScopes.none());
        }

        @Override
        public void invalidate(ObtainAccessTokenCommand command) {
            this.invalidated++;
        }
    }
}
