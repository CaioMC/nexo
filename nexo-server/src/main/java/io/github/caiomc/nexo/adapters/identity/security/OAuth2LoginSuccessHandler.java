package io.github.caiomc.nexo.adapters.identity.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import io.github.caiomc.nexo.adapters.identity.security.mapper.OAuth2SignInMapper;
import io.github.caiomc.nexo.core.identity.domain.model.SignIn;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.usecase.GetCurrentUserUseCase;
import io.github.caiomc.nexo.core.identity.usecase.SignInWithGoogleUseCase;
import io.github.caiomc.nexo.core.workspace.domain.exception.ScopesNotGrantedException;
import io.github.caiomc.nexo.core.workspace.usecase.CompleteConsentUseCase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Volta do Google (doc 04, seções 3 e 4). O Spring Security já validou o ID token (assinatura,
 * {@code iss}, {@code aud}, {@code nonce}, {@code exp}) e trocou o id da sessão. Aqui:
 *
 * <ol>
 *   <li>consentimento de contexto com outra conta Google → recusa ({@code account_mismatch});</li>
 *   <li>login no core (usuário + refresh token cifrado);</li>
 *   <li>sem refresh token guardado → refaz uma vez com {@code prompt=consent};</li>
 *   <li>troca a autenticação da sessão por {@link SessionUser} (sem token nenhum);</li>
 *   <li>se era consentimento de contexto, liga o contexto e volta para a tela de contextos.</li>
 * </ol>
 */
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final SignInWithGoogleUseCase signIn;
    private final GetCurrentUserUseCase currentUser;
    private final CompleteConsentUseCase consent;
    private final SecurityContextRepository contexts;
    private final OAuth2SignInMapper mapper;
    private final String webOrigin;

    public OAuth2LoginSuccessHandler(
            SignInWithGoogleUseCase signIn,
            GetCurrentUserUseCase currentUser,
            CompleteConsentUseCase consent,
            SecurityContextRepository contexts,
            OAuth2SignInMapper mapper,
            String webOrigin
    ) {
        this.signIn = signIn;
        this.currentUser = currentUser;
        this.consent = consent;
        this.contexts = contexts;
        this.mapper = mapper;
        this.webOrigin = webOrigin;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OidcUser google = (OidcUser) authentication.getPrincipal();
        OAuth2AuthorizedClient client = DiscardingAuthorizedClientRepository.take(request);
        HttpSession session = request.getSession();
        String pendingContext = (String) session.getAttribute(OAuthSessionAttributes.PENDING_CONTEXT);
        UUID pendingUser = (UUID) session.getAttribute(OAuthSessionAttributes.PENDING_USER);

        session.removeAttribute(OAuthSessionAttributes.PENDING_CONTEXT);
        session.removeAttribute(OAuthSessionAttributes.PENDING_USER);

        if (nonNull(pendingUser)) {
            User expected = this.currentUser.get(this.mapper.toCurrentUserCommand(pendingUser));

            if (!expected.googleSubject().equals(google.getSubject())) {
                log.warn("Consentimento de {} voltou com outra conta Google: recusado", pendingUser);
                this.authenticate(request, response, expected);
                this.redirect(response, "/settings/contexts?denied=" + this.encode(pendingContext) + "&reason=account_mismatch");

                return;
            }
        }

        SignIn result = this.signIn.signIn(this.mapper.toCommand(google, client));

        if (!result.offlineAccess() && isNull(session.getAttribute(OAuthSessionAttributes.CONSENT_RETRIED))) {
            session.setAttribute(OAuthSessionAttributes.CONSENT_RETRIED, true);
            response.sendRedirect("/oauth2/authorization/google?consent=true");

            return;
        }

        this.authenticate(request, response, result.user());

        if (isNull(pendingContext)) {
            this.redirect(response, "/");

            return;
        }

        try {
            this.consent.complete(this.mapper.toConsentCommand(result.user(), pendingContext, client));
            this.redirect(response, "/settings/contexts?enabled=" + this.encode(pendingContext));
        } catch (ScopesNotGrantedException e) {
            log.info("{}", e.getMessage());
            this.redirect(response, "/settings/contexts?denied=" + this.encode(pendingContext) + "&reason=scopes_not_granted");
        }
    }

    /**
     * A sessão passa a guardar só {@link SessionUser}: nada de ID token nem access token.
     */
    private void authenticate(
            HttpServletRequest request,
            HttpServletResponse response,
            User user
    ) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(this.mapper.toSessionUser(user), null, AuthorityUtils.createAuthorityList("ROLE_USER")));
        SecurityContextHolder.setContext(context);
        this.contexts.saveContext(context, request, response);
    }

    private void redirect(
            HttpServletResponse response,
            String path
    ) throws IOException {
        response.sendRedirect(this.webOrigin + path);
    }

    private String encode(String value) {
        return URLEncoder.encode(isNull(value) ? "" : value, StandardCharsets.UTF_8);
    }
}
