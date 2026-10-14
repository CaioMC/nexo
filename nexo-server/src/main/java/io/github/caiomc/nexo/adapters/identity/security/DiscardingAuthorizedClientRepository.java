package io.github.caiomc.nexo.adapters.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * O Spring Security não guarda token nenhum: o cliente autorizado do login fica só na requisição
 * do callback, para o {@link OAuth2LoginSuccessHandler} levar o refresh token, cifrado, para
 * {@code google_credential}. É o único lugar com token do Google.
 */
public class DiscardingAuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    private static final String ATTRIBUTE = DiscardingAuthorizedClientRepository.class.getName() + ".client";

    public static OAuth2AuthorizedClient take(HttpServletRequest request) {
        OAuth2AuthorizedClient client = (OAuth2AuthorizedClient) request.getAttribute(ATTRIBUTE);

        request.removeAttribute(ATTRIBUTE);

        return client;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request
    ) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient,
            Authentication principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        request.setAttribute(ATTRIBUTE, authorizedClient);
    }

    @Override
    public void removeAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
    }
}
