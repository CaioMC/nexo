package io.github.caiomc.nexo.adapters.identity.security;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.domain.exception.ContextNotFoundException;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

class IncrementalAuthorizationRequestResolverTest {

    private static final String CALENDAR = "https://www.googleapis.com/auth/calendar";

    private final IncrementalAuthorizationRequestResolver resolver = new IncrementalAuthorizationRequestResolver(
            new InMemoryClientRegistrationRepository(
                    CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("app").clientSecret("segredo").scope("openid", "email", "profile").build()
            ),
            command -> {
                if (!"calendar".equals(command.context())) {
                    throw new ContextNotFoundException(command.context());
                }

                return new GoogleContext(ContextKey.of("calendar"), "Calendar", "Agenda", "calendar-json.googleapis.com", 6, Set.of(CALENDAR));
            }
    );

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void plainLoginUsesPkceOfflineAccessAndSelectAccount() {
        OAuth2AuthorizationRequest request = this.resolver.resolve(this.request(null));

        assertThat(request.getScopes()).containsExactlyInAnyOrder("openid", "email", "profile");
        assertThat(request.getAdditionalParameters())
                .containsEntry("access_type", "offline")
                .containsEntry("prompt", "select_account")
                .containsEntry("include_granted_scopes", "true")
                .containsKey("code_challenge")
                .containsEntry("code_challenge_method", "S256");
    }

    @Test
    void contextAsksOnlyForItsScopesWithConsentAndRemembersWhoAsked() {
        UUID ana = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new SessionUser(ana, "ana@exemplo.com"), null, Set.of()));

        MockHttpServletRequest http = this.request("calendar");
        OAuth2AuthorizationRequest request = this.resolver.resolve(http);

        assertThat(request.getScopes()).contains(CALENDAR, "openid");
        assertThat(request.getAdditionalParameters())
                .containsEntry("prompt", "consent")
                .containsEntry("include_granted_scopes", "true")
                .containsEntry("login_hint", "ana@exemplo.com");
        assertThat(http.getSession().getAttribute(OAuthSessionAttributes.PENDING_CONTEXT)).isEqualTo("calendar");
        assertThat(http.getSession().getAttribute(OAuthSessionAttributes.PENDING_USER)).isEqualTo(ana);
    }

    @Test
    void unknownContextIsAPlainLogin() {
        MockHttpServletRequest http = this.request("fax");

        assertThat(this.resolver.resolve(http).getScopes()).doesNotContain(CALENDAR);
        assertThat(http.getSession().getAttribute(OAuthSessionAttributes.PENDING_CONTEXT)).isNull();
    }

    private MockHttpServletRequest request(String context) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");

        request.setServletPath("/oauth2/authorization/google");

        if (nonNull(context)) {
            request.setParameter("context", context);
        }

        return request;
    }
}
