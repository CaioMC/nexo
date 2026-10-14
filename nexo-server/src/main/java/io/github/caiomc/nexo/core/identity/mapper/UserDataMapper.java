package io.github.caiomc.nexo.core.identity.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.identity.domain.model.SignIn;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.port.repository.data.UserData;
import io.github.caiomc.nexo.core.identity.usecase.command.SignInCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * {@link UserData} ⇄ {@link User}.
 */
@Component
public class UserDataMapper {

    public User toUser(UserData data) {
        return new User(
                UserId.of(data.id()),
                data.googleSubject(),
                data.email(),
                data.displayName(),
                data.pictureUrl(),
                data.createdAt(),
                data.lastLoginAt()
        );
    }

    public UserData toData(User user) {
        return new UserData(
                user.id().value(),
                user.googleSubject(),
                user.email(),
                user.displayName(),
                user.pictureUrl(),
                user.createdAt(),
                user.lastLoginAt()
        );
    }

    public User firstLogin(
            SignInCommand command,
            Instant now
    ) {
        return User.firstLogin(
                command.googleSubject(),
                command.email(),
                command.displayName(),
                command.pictureUrl(),
                now
        );
    }

    public User loggedIn(
            User user,
            SignInCommand command,
            Instant now
    ) {
        return user.loggedIn(
                command.email(),
                command.displayName(),
                command.pictureUrl(),
                now
        );
    }

    public SignIn toSignIn(
            User user,
            boolean offlineAccess
    ) {
        return new SignIn(user, offlineAccess);
    }
}
