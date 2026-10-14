package io.github.caiomc.nexo.adapters.workspace.web.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.workspace.web.mapper.ContextWebMapper;
import io.github.caiomc.nexo.adapters.workspace.web.response.ContextActivationResponse;
import io.github.caiomc.nexo.adapters.workspace.web.response.ContextResponse;
import io.github.caiomc.nexo.core.workspace.usecase.DisableContextUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.EnableContextUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.ListContextsUseCase;

/**
 * Contextos do usuário (doc 05): listar, ligar (com consentimento incremental) e desligar.
 */
@RestController
@RequestMapping("/api/contexts")
public class ContextController {

    private final ListContextsUseCase list;
    private final EnableContextUseCase enable;
    private final DisableContextUseCase disable;
    private final ContextWebMapper mapper;

    public ContextController(
            ListContextsUseCase list,
            EnableContextUseCase enable,
            DisableContextUseCase disable,
            ContextWebMapper mapper
    ) {
        this.list = list;
        this.enable = enable;
        this.disable = disable;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ContextResponse> list(@AuthenticationPrincipal SessionUser user) {
        return this.mapper.toResponses(
                this.list.list(
                        this.mapper.toListCommand(user)
                )
        );
    }

    @PostMapping("/{key}/enable")
    public ContextActivationResponse enable(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String key
    ) {
        return this.mapper.toResponse(
                this.enable.enable(
                        this.mapper.toEnableCommand(user, key)
                )
        );
    }

    @PostMapping("/{key}/disable")
    public ContextResponse disable(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String key
    ) {
        return this.mapper.toResponse(
                this.disable.disable(
                        this.mapper.toDisableCommand(user, key)
                )
        );
    }
}
