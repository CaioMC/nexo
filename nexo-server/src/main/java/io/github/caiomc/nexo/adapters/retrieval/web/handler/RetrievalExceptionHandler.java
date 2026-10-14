package io.github.caiomc.nexo.adapters.retrieval.web.handler;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.caiomc.nexo.adapters.retrieval.web.controller.SearchController;
import io.github.caiomc.nexo.adapters.shared.openai.exception.OpenAiApiException;

@RestControllerAdvice(basePackageClasses = SearchController.class)
public class RetrievalExceptionHandler {

    private static final String PROBLEMS = "https://nexo.dev/problems/";

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return this.problem(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida", e.getMessage());
    }

    @ExceptionHandler(OpenAiApiException.class)
    public ProblemDetail upstream(OpenAiApiException e) {
        return this.problem(HttpStatus.BAD_GATEWAY, "llm-indisponivel", "Falha na API de embeddings", e.getMessage());
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
