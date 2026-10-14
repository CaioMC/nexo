package io.github.caiomc.nexo.adapters.workspace.web.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.caiomc.nexo.adapters.shared.web.handler.ProblemDetails;
import io.github.caiomc.nexo.adapters.workspace.web.controller.ContextController;
import io.github.caiomc.nexo.core.workspace.domain.exception.ContextNotFoundException;
import io.github.caiomc.nexo.core.workspace.domain.exception.ScopesNotGrantedException;

@RestControllerAdvice(basePackageClasses = ContextController.class)
public class WorkspaceExceptionHandler {

    @ExceptionHandler(ContextNotFoundException.class)
    public ProblemDetail notFound(ContextNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "contexto-nao-encontrado", "Contexto não encontrado", e.getMessage());
    }

    @ExceptionHandler(ScopesNotGrantedException.class)
    public ProblemDetail scopes(ScopesNotGrantedException e) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, "escopos-nao-concedidos", "Escopos não concedidos", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida", e.getMessage());
    }
}
