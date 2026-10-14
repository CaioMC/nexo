package io.github.caiomc.nexo.adapters.identity.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.shared.web.handler.ProblemDetails;

/**
 * Sem sessão → 401 em Problem Details, sem redirect: o front decide ir para o login. CSRF ausente
 * ou errado → 403 {@code csrf-invalido}.
 */
public class ProblemSecurityHandlers {

    private final ObjectMapper json;

    public ProblemSecurityHandlers(ObjectMapper json) {
        this.json = json;
    }

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, exception) -> this.write(
                response,
                ProblemDetails.of(
                        HttpStatus.UNAUTHORIZED,
                        "nao-autenticado",
                        "Não autenticado",
                        "Entre com o Google para continuar"
                )
        );
    }

    public AccessDeniedHandler accessDenied() {
        return (request, response, exception) -> this.write(
                response,
                exception instanceof CsrfException ?
                        ProblemDetails.of(HttpStatus.FORBIDDEN, "csrf-invalido", "CSRF inválido", "Envie o cabeçalho X-XSRF-TOKEN com o valor do cookie XSRF-TOKEN") :
                        ProblemDetails.of(HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado", exception.getMessage())
        );
    }

    private void write(
            HttpServletResponse response,
            ProblemDetail problem
    ) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        this.json.writeValue(response.getOutputStream(), problem);
    }
}
