package io.github.caiomc.nexo.core.identity.mapper;

import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.identity.domain.model.GoogleCredential;
import io.github.caiomc.nexo.core.identity.domain.model.GrantedScopes;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RefreshTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RevokeTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.data.TokenData;
import io.github.caiomc.nexo.core.identity.port.repository.data.GoogleCredentialData;
import io.github.caiomc.nexo.core.identity.usecase.command.SignInCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * {@link GoogleCredentialData} ⇄ {@link GoogleCredential}, e os tokens que entram e saem das portas.
 */
@Component
public class GoogleCredentialDataMapper {

    public GoogleCredential toCredential(GoogleCredentialData data) {
        return new GoogleCredential(
                UserId.of(data.userId()),
                new EncryptedToken(data.refreshToken(), data.keyVersion()),
                GrantedScopes.of(data.grantedScopes()),
                data.updatedAt()
        );
    }

    public GoogleCredentialData toData(GoogleCredential credential) {
        return new GoogleCredentialData(
                credential.userId().value(),
                credential.refreshToken().bytes(),
                credential.refreshToken().keyVersion(),
                credential.scopes().sorted().stream().toList(),
                credential.updatedAt()
        );
    }

    public GoogleCredential newCredential(
            UserId userId,
            EncryptedToken refreshToken,
            Set<String> scopes,
            Instant now
    ) {
        return new GoogleCredential(
                userId,
                refreshToken,
                GrantedScopes.of(scopes),
                now
        );
    }

    public RefreshTokenCommand toRefreshCommand(
            UserId userId,
            String refreshToken
    ) {
        return new RefreshTokenCommand(userId, refreshToken);
    }

    public RevokeTokenCommand toRevokeCommand(String token) {
        return new RevokeTokenCommand(token);
    }

    public AccessToken toAccessToken(
            TokenData token,
            Instant now
    ) {
        return new AccessToken(
                token.accessToken(),
                now.plusSeconds(token.expiresInSeconds()),
                GrantedScopes.of(token.scopes())
        );
    }

    public AccessToken toAccessToken(SignInCommand command) {
        return new AccessToken(
                command.accessToken(),
                command.accessTokenExpiresAt(),
                GrantedScopes.of(command.grantedScopes())
        );
    }
}
