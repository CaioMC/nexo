package io.github.caiomc.nexo.adapters.identity.security;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import io.github.caiomc.nexo.core.workspace.domain.exception.ContextNotFoundException;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.usecase.FindContextUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.FindContextCommand;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Monta o pedido ao Google (doc 04, seções 3 e 4), sempre com PKCE e {@code access_type=offline}:
 *
 * <ul>
 *   <li>{@code ?context=calendar}: consentimento incremental, só com os escopos do contexto, somados
 *   aos já concedidos ({@code include_granted_scopes}), {@code prompt=consent} e {@code login_hint};</li>
 *   <li>{@code ?consent=true}: login de novo com {@code prompt=consent} (o Google não mandou refresh token);</li>
 *   <li>sem parâmetro: login comum com {@code prompt=select_account}.</li>
 * </ul>
 */
public class IncrementalAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final FindContextUseCase contexts;

    public IncrementalAuthorizationRequestResolver(
            ClientRegistrationRepository registrations,
            FindContextUseCase contexts
    ) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, AUTHORIZATION_BASE_URI);
        this.delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        this.contexts = contexts;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return this.customize(request, this.delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request,
            String clientRegistrationId
    ) {
        return this.customize(request, this.delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorization
    ) {
        if (isNull(authorization)) {
            return null;
        }

        Set<String> scopes = new LinkedHashSet<>(authorization.getScopes());
        Map<String, Object> parameters = new LinkedHashMap<>(authorization.getAdditionalParameters());
        HttpSession session = request.getSession();
        GoogleContext context = this.context(request.getParameter("context"));

        parameters.put("access_type", "offline");
        parameters.put("include_granted_scopes", "true");
        session.removeAttribute(OAuthSessionAttributes.PENDING_CONTEXT);
        session.removeAttribute(OAuthSessionAttributes.PENDING_USER);

        if (nonNull(context)) {
            scopes.addAll(context.scopes());
            parameters.put("prompt", "consent");
            session.setAttribute(OAuthSessionAttributes.PENDING_CONTEXT, context.key().value());
            this.rememberCurrentUser(session, parameters);
        } else if ("true".equals(request.getParameter("consent"))) {
            parameters.put("prompt", "consent");
        } else {
            parameters.put("prompt", "select_account");
            session.removeAttribute(OAuthSessionAttributes.CONSENT_RETRIED);
        }

        return OAuth2AuthorizationRequest.from(authorization)
                .scopes(scopes)
                .additionalParameters(parameters)
                .build();
    }

    /**
     * Quem pediu o consentimento: na volta, a conta do Google precisa ser a mesma ({@code account_mismatch}).
     */
    private void rememberCurrentUser(
            HttpSession session,
            Map<String, Object> parameters
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (nonNull(authentication) && authentication.getPrincipal() instanceof SessionUser(var userId, var email)) {
            session.setAttribute(OAuthSessionAttributes.PENDING_USER, userId);
            parameters.put("login_hint", email);
        }
    }

    private GoogleContext context(String key) {
        if (isNull(key) || key.isBlank()) {
            return null;
        }

        try {
            return this.contexts.find(new FindContextCommand(key));
        } catch (ContextNotFoundException e) {
            return null;
        }
    }
}
