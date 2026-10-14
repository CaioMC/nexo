package io.github.caiomc.nexo.adapters.catalog.web.handler;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.caiomc.nexo.adapters.catalog.web.controller.CatalogController;
import io.github.caiomc.nexo.core.catalog.domain.exception.ToolNotFoundException;

@RestControllerAdvice(basePackageClasses = CatalogController.class)
public class CatalogExceptionHandler {

    private static final String PROBLEMS = "https://nexo.dev/problems/";

    @ExceptionHandler(ToolNotFoundException.class)
    public ProblemDetail notFound(ToolNotFoundException e) {
        return this.problem(HttpStatus.NOT_FOUND, "ferramenta-nao-encontrada", "Ferramenta não encontrada", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return this.problem(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida", e.getMessage());
    }

    private ProblemDetail problem(
            HttpStatus status,
            String type,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setType(URI.create(PROBLEMS + type));
        problem.setTitle(title);

        return problem;
    }
}
