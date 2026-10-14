package io.github.caiomc.nexo.core.identity.usecase;

import io.github.caiomc.nexo.core.identity.domain.model.SignIn;
import io.github.caiomc.nexo.core.identity.usecase.command.SignInCommand;

public interface SignInWithGoogleUseCase {

    SignIn signIn(SignInCommand command);
}
