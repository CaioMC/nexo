package io.github.caiomc.nexo.adapters.identity.google;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.port.oauth.GoogleOAuthPort;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RefreshTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RevokeTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.data.TokenData;

/**
 * Refresh e revogação no Google, fora do fluxo de login (que é do Spring Security). O cliente
 * OAuth (id e segredo) é o mesmo do login.
 */
public class GoogleOAuthAdapter implements GoogleOAuthPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthAdapter.class);

    private static final String REGISTRATION = "google";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final ClientRegistrationRepository registrations;
    private final URI tokenUri;
    private final URI revokeUri;
    private final ObjectMapper json;
    private final HttpClient http;

    public GoogleOAuthAdapter(
            ClientRegistrationRepository registrations,
            URI tokenUri,
            URI revokeUri,
            ObjectMapper json
    ) {
        this.registrations = registrations;
        this.tokenUri = tokenUri;
        this.revokeUri = revokeUri;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public TokenData refresh(RefreshTokenCommand command) {
        ClientRegistration client = this.registrations.findByRegistrationId(REGISTRATION);
        Map<String, String> form = new LinkedHashMap<>();

        form.put("grant_type", "refresh_token");
        form.put("refresh_token", command.refreshToken());
        form.put("client_id", client.getClientId());
        form.put("client_secret", client.getClientSecret());

        HttpResponse<String> response = this.post(this.tokenUri, form);
        JsonNode body = this.read(response.body());

        if (response.statusCode() == 400 && "invalid_grant".equals(body.path("error").asText())) {
            throw new ConsentRevokedException(command.userId());
        }

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Refresh no Google falhou (" + response.statusCode() + "): " + body.path("error").asText() + " " + body.path("error_description").asText());
        }

        return new TokenData(
                body.path("access_token").asText(),
                body.path("expires_in").asLong(3600),
                this.scopes(body.path("scope").asText(""))
        );
    }

    @Override
    public void revoke(RevokeTokenCommand command) {
        try {
            HttpResponse<String> response = this.post(this.revokeUri, Map.of("token", command.token()));

            if (response.statusCode() / 100 != 2) {
                log.warn("Revogação no Google respondeu {}: seguindo com o desconectar local", response.statusCode());
            }
        } catch (IllegalStateException e) {
            log.warn("Revogação no Google falhou: {}. Seguindo com o desconectar local", e.getMessage());
        }
    }

    private HttpResponse<String> post(
            URI uri,
            Map<String, String> form
    ) {
        String body = form.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest
                .newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            return this.http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Sem comunicação com " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada OAuth ao Google interrompida", e);
        }
    }

    private JsonNode read(String body) {
        try {
            return this.json.readTree(body.isBlank() ? "{}" : body);
        } catch (IOException e) {
            return this.json.createObjectNode();
        }
    }

    private Set<String> scopes(String scope) {
        return Arrays.stream(scope.split(" ")).filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }
}
