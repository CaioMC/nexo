package io.github.caiomc.nexo.core.workspace.mapper;

import java.time.Instant;
import java.util.Collection;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextActivation;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextState;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.domain.model.UserContext;
import io.github.caiomc.nexo.core.workspace.port.consent.command.ConsentUrlCommand;
import io.github.caiomc.nexo.core.workspace.port.notification.command.NotifyContextChangeCommand;
import io.github.caiomc.nexo.core.workspace.port.repository.data.UserContextData;

/**
 * {@link UserContextData} ⇄ {@link UserContext}, e os objetos que o serviço entrega às portas e à borda.
 */
@Component
public class UserContextDataMapper {

    public UserContext toUserContext(UserContextData data) {
        return new UserContext(
                UserId.of(data.userId()),
                ContextKey.of(data.contextKey()),
                data.enabled(),
                data.changedAt()
        );
    }

    public UserContextData toData(
            UserId userId,
            ContextKey context,
            boolean enabled,
            Instant now
    ) {
        UserContext switched = UserContext.switched(
                userId,
                context,
                enabled,
                now
        );

        return new UserContextData(
                switched.userId().value(),
                switched.context().value(),
                switched.enabled(),
                switched.changedAt()
        );
    }

    public ContextState toState(
            GoogleContext context,
            boolean enabled,
            Collection<String> grantedScopes,
            int toolCount
    ) {
        return new ContextState(
                context,
                enabled,
                context.grantedBy(grantedScopes),
                toolCount
        );
    }

    public ContextActivation enabled(ContextKey context) {
        return new ContextActivation.Enabled(context);
    }

    public ContextActivation consentRequired(
            ContextKey context,
            String authorizationUrl
    ) {
        return new ContextActivation.ConsentRequired(context, authorizationUrl);
    }

    public ConsentUrlCommand toConsentUrlCommand(ContextKey context) {
        return new ConsentUrlCommand(context);
    }

    public NotifyContextChangeCommand toNotifyCommand(
            UserId userId,
            ContextKey context,
            boolean enabled
    ) {
        return new NotifyContextChangeCommand(
                userId,
                context,
                enabled
        );
    }
}
