package io.github.caiomc.nexo.adapters.identity.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Usuário recusou no Google ou o callback veio inválido: volta para a tela de login do front.
 */
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final String webOrigin;

    public OAuth2LoginFailureHandler(String webOrigin) {
        this.webOrigin = webOrigin;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        log.info("Login com Google não concluído: {}", exception.getMessage());
        response.sendRedirect(this.webOrigin + "/login?error=google");
    }
}
