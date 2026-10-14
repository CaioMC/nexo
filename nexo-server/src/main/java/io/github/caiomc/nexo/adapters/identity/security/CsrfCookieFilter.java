package io.github.caiomc.nexo.adapters.identity.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import static java.util.Objects.nonNull;

/**
 * O token de CSRF é carregado sob demanda; ler o valor aqui faz o cookie {@code XSRF-TOKEN} sair
 * já na primeira resposta, para o front ter o que mandar no cabeçalho {@code X-XSRF-TOKEN}.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (nonNull(token)) {
            token.getToken();
        }

        chain.doFilter(request, response);
    }
}
