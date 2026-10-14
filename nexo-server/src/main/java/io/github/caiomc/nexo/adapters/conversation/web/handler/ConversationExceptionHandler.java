package io.github.caiomc.nexo.adapters.conversation.web.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.github.caiomc.nexo.adapters.conversation.web.controller.ConversationController;
import io.github.caiomc.nexo.adapters.shared.web.handler.ProblemDetails;
import io.github.caiomc.nexo.core.conversation.domain.exception.ConversationNotFoundException;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnInProgressException;

@RestControllerAdvice(basePackageClasses = ConversationController.class)
public class ConversationExceptionHandler {

    @ExceptionHandler(ConversationNotFoundException.class)
    public ProblemDetail notFound(ConversationNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "conversa-nao-encontrada", "Conversa não encontrada", e.getMessage());
    }

    @ExceptionHandler(TurnInProgressException.class)
    public ProblemDetail conflict(TurnInProgressException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "turno-em-andamento", "Turno em andamento", e.getMessage() + " Pare o turno antes.");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail badRequest(Exception e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida", e.getMessage());
    }
}
