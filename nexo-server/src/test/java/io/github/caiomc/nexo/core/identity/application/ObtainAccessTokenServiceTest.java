package io.github.caiomc.nexo.core.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.domain.exception.GoogleCredentialMissingException;
import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.identity.domain.model.GrantedScopes;
import io.github.caiomc.nexo.core.identity.mapper.GoogleCredentialDataMapper;
import io.github.caiomc.nexo.core.identity.mapper.IdentityCommandMapper;
import io.github.caiomc.nexo.core.identity.port.repository.data.GoogleCredentialData;
import io.github.caiomc.nexo.core.identity.usecase.command.ObtainAccessTokenCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.support.IdentityTestDoubles.InMemoryCredentials;
import io.github.caiomc.nexo.core.support.IdentityTestDoubles.MapTokenCache;
import io.github.caiomc.nexo.core.support.IdentityTestDoubles.PlainCipher;
import io.github.caiomc.nexo.core.support.IdentityTestDoubles.RecordingReconcile;
import io.github.caiomc.nexo.core.support.IdentityTestDoubles.ScriptedOAuth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObtainAccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");
    private static final String CALENDAR = "https://www.googleapis.com/auth/calendar";
    private static final String GMAIL = "https://www.googleapis.com/auth/gmail.modify";

    private final UserId ana = UserId.of(UUID.randomUUID());
    private final MapTokenCache cache = new MapTokenCache();
    private final InMemoryCredentials credentials = new InMemoryCredentials();
    private final PlainCipher cipher = new PlainCipher();
    private final ScriptedOAuth oauth = new ScriptedOAuth();
    private final RecordingReconcile reconcile = new RecordingReconcile();

    private final ObtainAccessTokenService service = new ObtainAccessTokenService(
            this.cache,
            this.credentials,
            this.cipher,
            this.oauth,
            this.reconcile,
            new GoogleCredentialDataMapper(),
            new IdentityCommandMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @BeforeEach
    void storedCredential() {
        EncryptedToken token = this.cipher.encrypt("refresh-1", this.ana);

        this.credentials.save(new GoogleCredentialData(this.ana.value(), token.bytes(), 1, List.of(CALENDAR, GMAIL), NOW));
    }

    @Test
    void cachedTokenValidForMoreThanAMinuteIsReused() {
        this.cache.put(this.ana, new AccessToken("em-cache", NOW.plusSeconds(120), GrantedScopes.none()));

        assertThat(this.service.obtain(new ObtainAccessTokenCommand(this.ana)).value()).isEqualTo("em-cache");
        assertThat(this.oauth.refreshed).isEmpty();
    }

    @Test
    void almostExpiredTokenIsRefreshedAndScopesThatDisappearedTurnContextsOff() {
        this.cache.put(this.ana, new AccessToken("vencendo", NOW.plusSeconds(30), GrantedScopes.none()));

        AccessToken token = this.service.obtain(new ObtainAccessTokenCommand(this.ana));

        assertThat(token.value()).isEqualTo("access-1");
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(this.oauth.refreshed).containsExactly("refresh-1");
        assertThat(this.credentials.rows.get(this.ana.value()).grantedScopes()).containsExactly(CALENDAR);
        assertThat(this.reconcile.received).singleElement().satisfies(command -> assertThat(command.grantedScopes()).isEqualTo(Set.of(CALENDAR)));
        assertThat(this.cache.tokens.get(this.ana).value()).isEqualTo("access-1");
    }

    @Test
    void keyRotationReencryptsOnRefresh() {
        this.oauth.scopes = Set.of(CALENDAR, GMAIL);
        this.cipher.version = 2;

        this.service.obtain(new ObtainAccessTokenCommand(this.ana));

        assertThat(this.credentials.rows.get(this.ana.value()).keyVersion()).isEqualTo(2);
        assertThat(this.reconcile.received).isEmpty();
    }

    @Test
    void revokedRefreshTokenForgetsTheCredentialAndTurnsEverythingOff() {
        this.oauth.revokedByGoogle = true;

        assertThatThrownBy(() -> this.service.obtain(new ObtainAccessTokenCommand(this.ana))).isInstanceOf(ConsentRevokedException.class);
        assertThat(this.credentials.rows).isEmpty();
        assertThat(this.reconcile.received).singleElement().satisfies(command -> assertThat(command.grantedScopes()).isEmpty());
        assertThatThrownBy(() -> this.service.obtain(new ObtainAccessTokenCommand(this.ana))).isInstanceOf(GoogleCredentialMissingException.class);
    }
}
