package io.github.caiomc.nexo.adapters.identity.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.identity.web.mapper.IdentityWebMapper;
import io.github.caiomc.nexo.core.identity.usecase.DisconnectGoogleUseCase;

import static java.util.Objects.nonNull;

/**
 * Sair (a credencial fica: o próximo login não pede consentimento) e desconectar (revoga no
 * Google, apaga a credencial e desliga os contextos). RF-04.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DisconnectGoogleUseCase disconnect;
    private final IdentityWebMapper mapper;

    public AuthController(
            DisconnectGoogleUseCase disconnect,
            IdentityWebMapper mapper
    ) {
        this.disconnect = disconnect;
        this.mapper = mapper;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        this.invalidate(request);
    }

    @PostMapping("/disconnect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(
            @AuthenticationPrincipal SessionUser user,
            HttpServletRequest request
    ) {
        this.disconnect.disconnect(this.mapper.toDisconnectCommand(user));
        this.invalidate(request);
    }

    private void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (nonNull(session)) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
    }
}
