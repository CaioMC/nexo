package io.github.caiomc.nexo.adapters.identity.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.identity.web.mapper.IdentityWebMapper;
import io.github.caiomc.nexo.adapters.identity.web.response.MeResponse;
import io.github.caiomc.nexo.core.identity.usecase.GetCurrentUserUseCase;

/**
 * {@code GET /api/me}: quem está logado (RF-05).
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final GetCurrentUserUseCase currentUser;
    private final IdentityWebMapper mapper;

    public MeController(
            GetCurrentUserUseCase currentUser,
            IdentityWebMapper mapper
    ) {
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal SessionUser user) {
        return this.mapper.toResponse(
                this.currentUser.get(
                        this.mapper.toCurrentUserCommand(user)
                )
        );
    }
}
