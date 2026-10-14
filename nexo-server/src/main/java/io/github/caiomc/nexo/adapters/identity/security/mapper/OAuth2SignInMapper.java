package io.github.caiomc.nexo.adapters.identity.security.mapper;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.usecase.command.GetCurrentUserCommand;
import io.github.caiomc.nexo.core.identity.usecase.command.SignInCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.command.CompleteConsentCommand;

import static java.util.Objects.isNull;

/**
 * O que o Spring Security entrega no callback (ID token + cliente autorizado) → comandos do core.
 */
@Component
public class OAuth2SignInMapper {

    public SignInCommand toCommand(
            OidcUser user,
            OAuth2AuthorizedClient client
    ) {
        OAuth2AccessToken access = isNull(client) ? null : client.getAccessToken();
        OAuth2RefreshToken refresh = isNull(client) ? null : client.getRefreshToken();

        return new SignInCommand(
                user.getSubject(),
                user.getEmail(),
                isNull(user.getFullName()) ? "" : user.getFullName(),
                user.getPicture(),
                isNull(access) ? null : access.getTokenValue(),
                isNull(access) ? null : access.getExpiresAt(),
                isNull(refresh) ? null : refresh.getTokenValue(),
                isNull(access) ? Set.of() : access.getScopes()
        );
    }

    public CompleteConsentCommand toConsentCommand(
            User user,
            String context,
            OAuth2AuthorizedClient client
    ) {
        return new CompleteConsentCommand(
                user.id(),
                context,
                isNull(client) ? Set.of() : client.getAccessToken().getScopes()
        );
    }

    public GetCurrentUserCommand toCurrentUserCommand(UUID userId) {
        return new GetCurrentUserCommand(UserId.of(userId));
    }

    public SessionUser toSessionUser(User user) {
        return new SessionUser(user.id().value(), user.email());
    }
}
