package io.github.caiomc.nexo.adapters.identity.security;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import io.github.caiomc.nexo.adapters.identity.security.mapper.OAuth2SignInMapper;
import io.github.caiomc.nexo.core.identity.domain.model.SignIn;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.usecase.GetCurrentUserUseCase;
import io.github.caiomc.nexo.core.identity.usecase.SignInWithGoogleUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.SignInCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.domain.exception.ScopesNotGrantedException;
import io.github.caiomc.nexo.core.workspace.usecase.CompleteConsentUseCase;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    private static final String WEB = "http://localhost:5173";
    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final SignInWithGoogleUseCase signIn = mock(SignInWithGoogleUseCase.class);
    private final GetCurrentUserUseCase currentUser = mock(GetCurrentUserUseCase.class);
    private final CompleteConsentUseCase consent = mock(CompleteConsentUseCase.class);

    private final OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(
            this.signIn,
            this.currentUser,
            this.consent,
            new HttpSessionSecurityContextRepository(),
            new OAuth2SignInMapper(),
            WEB
    );

    private final User ana = new User(UserId.of(UUID.randomUUID()), "sub-ana", "ana@exemplo.com", "Ana", null, NOW, NOW);

    @Test
    void loginStoresOnlySessionUserAndGoesToTheFront() throws Exception {
        when(this.signIn.signIn(any(SignInCommand.class))).thenReturn(new SignIn(this.ana, true));

        MockHttpServletRequest request = this.callback("refresh-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.handler.onAuthenticationSuccess(request, response, this.authentication("sub-ana"));

        assertThat(response.getRedirectedUrl()).isEqualTo(WEB + "/");
        assertThat(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY).toString())
                .contains("SessionUser")
                .doesNotContain("access-1")
                .doesNotContain("refresh-1");
    }

    @Test
    void withoutRefreshTokenAsksForConsentOnce() throws Exception {
        when(this.signIn.signIn(any(SignInCommand.class))).thenReturn(new SignIn(this.ana, false));

        MockHttpServletRequest request = this.callback(null);
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();

        this.handler.onAuthenticationSuccess(request, first, this.authentication("sub-ana"));
        this.handler.onAuthenticationSuccess(request, second, this.authentication("sub-ana"));

        assertThat(first.getRedirectedUrl()).isEqualTo("/oauth2/authorization/google?consent=true");
        assertThat(second.getRedirectedUrl()).isEqualTo(WEB + "/");
    }

    @Test
    void consentForAContextEnablesItOrSaysItWasDenied() throws Exception {
        when(this.signIn.signIn(any(SignInCommand.class))).thenReturn(new SignIn(this.ana, true));
        when(this.currentUser.get(any())).thenReturn(this.ana);

        MockHttpServletResponse enabled = this.consentCallback();

        assertThat(enabled.getRedirectedUrl()).isEqualTo(WEB + "/settings/contexts?enabled=calendar");

        doThrow(new ScopesNotGrantedException(ContextKey.of("calendar"), Set.of("x"))).when(this.consent).complete(any());

        assertThat(this.consentCallback().getRedirectedUrl()).isEqualTo(WEB + "/settings/contexts?denied=calendar&reason=scopes_not_granted");
    }

    @Test
    void consentWithAnotherGoogleAccountIsRefused() throws Exception {
        when(this.currentUser.get(any())).thenReturn(this.ana);

        MockHttpServletRequest request = this.callback("refresh-x");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.getSession().setAttribute(OAuthSessionAttributes.PENDING_CONTEXT, "calendar");
        request.getSession().setAttribute(OAuthSessionAttributes.PENDING_USER, this.ana.id().value());
        this.handler.onAuthenticationSuccess(request, response, this.authentication("sub-outra-conta"));

        assertThat(response.getRedirectedUrl()).isEqualTo(WEB + "/settings/contexts?denied=calendar&reason=account_mismatch");
        verify(this.signIn, never()).signIn(any());
    }

    private MockHttpServletResponse consentCallback() throws Exception {
        MockHttpServletRequest request = this.callback("refresh-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.getSession().setAttribute(OAuthSessionAttributes.PENDING_CONTEXT, "calendar");
        request.getSession().setAttribute(OAuthSessionAttributes.PENDING_USER, this.ana.id().value());
        this.handler.onAuthenticationSuccess(request, response, this.authentication("sub-ana"));

        return response;
    }

    private MockHttpServletRequest callback(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("app").clientSecret("segredo").build();
        OAuth2AccessToken access = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-1", NOW, NOW.plusSeconds(3600), Set.of("openid", "https://www.googleapis.com/auth/calendar"));
        OAuth2RefreshToken refresh = isNull(refreshToken) ? null : new OAuth2RefreshToken(refreshToken, NOW);

        new DiscardingAuthorizedClientRepository().saveAuthorizedClient(new OAuth2AuthorizedClient(registration, "sub-ana", access, refresh), null, request, null);

        return request;
    }

    private OAuth2AuthenticationToken authentication(String subject) {
        OidcIdToken token = new OidcIdToken("id-token", NOW, NOW.plusSeconds(3600), Map.of("sub", subject, "email", "ana@exemplo.com", "name", "Ana"));

        return new OAuth2AuthenticationToken(new DefaultOidcUser(Set.of(), token), Set.of(), "google");
    }
}
