package io.github.caiomc.nexo.support;

import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Usuário logado nos testes web: o mesmo {@link SessionUser} que o login com Google põe na sessão.
 */
public final class TestUsers {

    public static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-00000000a0a0");
    public static final SessionUser ANA = new SessionUser(ANA_ID, "ana@exemplo.com");

    private TestUsers() {
    }

    public static UserId anaId() {
        return UserId.of(ANA_ID);
    }

    public static RequestPostProcessor ana() {
        return as(ANA);
    }

    public static RequestPostProcessor as(SessionUser user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    }
}
