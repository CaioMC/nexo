package io.github.caiomc.nexo.adapters.shared.web.handler;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Problem Details (RFC 9457) com {@code type} = {@code https://nexo.dev/problems/<codigo>}.
 */
public final class ProblemDetails {

    public static final String PROBLEMS = "https://nexo.dev/problems/";

    private ProblemDetails() {
    }

    public static ProblemDetail of(
            HttpStatus status,
            String code,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setType(URI.create(PROBLEMS + code));
        problem.setTitle(title);

        return problem;
    }
}
