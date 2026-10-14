package io.github.caiomc.nexo.it.support;

import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;

/**
 * Só nos testes: {@code POST /test/login} faz o que o {@code OAuth2LoginSuccessHandler} faz depois
 * do Google (grava {@link SessionUser} na sessão JDBC) e devolve o cookie {@code SESSION} de verdade.
 */
@TestConfiguration
public class TestLoginConfig {

    @Bean
    @Order(0)
    SecurityFilterChain testLoginChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/test/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> testLogin() {
        return RouterFunctions.route()
                .POST("/test/login", request -> {
                    SessionUser user = new SessionUser(UUID.fromString(request.param("userId").orElseThrow()), request.param("email").orElseThrow());
                    SecurityContext context = SecurityContextHolder.createEmptyContext();

                    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
                    request.servletRequest().getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

                    return ServerResponse.ok().body("ok");
                })
                .build();
    }
}
