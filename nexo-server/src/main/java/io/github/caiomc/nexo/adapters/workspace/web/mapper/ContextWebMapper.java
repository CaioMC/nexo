package io.github.caiomc.nexo.adapters.workspace.web.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.workspace.web.response.ContextActivationResponse;
import io.github.caiomc.nexo.adapters.workspace.web.response.ContextResponse;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextActivation;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextState;
import io.github.caiomc.nexo.core.workspace.usecase.command.DisableContextCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.EnableContextCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.ListContextsCommand;

@Component
public class ContextWebMapper {

    public ListContextsCommand toListCommand(SessionUser user) {
        return new ListContextsCommand(UserId.of(user.userId()));
    }

    public EnableContextCommand toEnableCommand(
            SessionUser user,
            String key
    ) {
        return new EnableContextCommand(UserId.of(user.userId()), key);
    }

    public DisableContextCommand toDisableCommand(
            SessionUser user,
            String key
    ) {
        return new DisableContextCommand(UserId.of(user.userId()), key);
    }

    public List<ContextResponse> toResponses(List<ContextState> states) {
        return states.stream().map(this::toResponse).toList();
    }

    public ContextResponse toResponse(ContextState state) {
        return new ContextResponse(
                state.context().key().value(),
                state.context().name(),
                state.context().description(),
                state.enabled(),
                state.scopesGranted(),
                List.copyOf(state.context().sortedScopes()),
                state.toolCount()
        );
    }

    public ContextActivationResponse toResponse(ContextActivation activation) {
        return switch (activation) {
            case ContextActivation.Enabled(var context) -> new ContextActivationResponse(ContextActivationResponse.ENABLED, context.value(), null);
            case ContextActivation.ConsentRequired(var context, var url) -> new ContextActivationResponse(ContextActivationResponse.CONSENT_REQUIRED, context.value(), url);
        };
    }
}
