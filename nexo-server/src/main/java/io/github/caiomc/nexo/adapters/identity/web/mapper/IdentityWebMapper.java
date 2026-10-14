package io.github.caiomc.nexo.adapters.identity.web.mapper;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.identity.web.response.MeResponse;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.usecase.command.DisconnectGoogleCommand;
import io.github.caiomc.nexo.core.identity.usecase.command.GetCurrentUserCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

@Component
public class IdentityWebMapper {

    public GetCurrentUserCommand toCurrentUserCommand(SessionUser user) {
        return new GetCurrentUserCommand(UserId.of(user.userId()));
    }

    public DisconnectGoogleCommand toDisconnectCommand(SessionUser user) {
        return new DisconnectGoogleCommand(UserId.of(user.userId()));
    }

    public MeResponse toResponse(User user) {
        return new MeResponse(
                user.id().value(),
                user.email(),
                user.displayName(),
                user.pictureUrl()
        );
    }
}
