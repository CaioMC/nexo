package io.github.caiomc.nexo.adapters.identity.web.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.caiomc.nexo.adapters.identity.web.controller.MeController;
import io.github.caiomc.nexo.adapters.shared.web.handler.ProblemDetails;
import io.github.caiomc.nexo.core.identity.domain.exception.UserNotFoundException;

@RestControllerAdvice(basePackageClasses = MeController.class)
public class IdentityExceptionHandler {

    /**
     * Sessão de um usuário que não existe mais (apagou os dados): vale como não autenticado.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail gone(UserNotFoundException e) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado", e.getMessage());
    }
}
