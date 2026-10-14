package io.github.caiomc.nexo.adapters.identity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.identity.config.IdentityProperties;
import io.github.caiomc.nexo.adapters.identity.security.mapper.OAuth2SignInMapper;
import io.github.caiomc.nexo.core.identity.usecase.GetCurrentUserUseCase;
import io.github.caiomc.nexo.core.identity.usecase.SignInWithGoogleUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.CompleteConsentUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.FindContextUseCase;

/**
 * Login só Google (OIDC + PKCE) no padrão BFF (doc 04): o navegador só recebe o cookie de sessão.
 *
 * <ul>
 *   <li>{@code /oauth2/**} e {@code /login/oauth2/**}: fluxo OAuth do Spring Security;</li>
 *   <li>{@code /api/**} e {@code /ws/**}: exigem sessão; sem ela, 401 em Problem Details;</li>
 *   <li>CSRF por cookie {@code XSRF-TOKEN} + cabeçalho {@code X-XSRF-TOKEN} nas rotas que alteram estado.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    DiscardingAuthorizedClientRepository discardingAuthorizedClientRepository() {
        return new DiscardingAuthorizedClientRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository registrations,
            FindContextUseCase contexts,
            SignInWithGoogleUseCase signIn,
            GetCurrentUserUseCase currentUser,
            CompleteConsentUseCase consent,
            SecurityContextRepository securityContexts,
            DiscardingAuthorizedClientRepository authorizedClients,
            OAuth2SignInMapper mapper,
            IdentityProperties identity,
            ObjectMapper json
    ) throws Exception {
        ProblemSecurityHandlers problems = new ProblemSecurityHandlers(json);
        OAuth2LoginSuccessHandler success = new OAuth2LoginSuccessHandler(
                signIn,
                currentUser,
                consent,
                securityContexts,
                mapper,
                identity.webOrigin()
        );

        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/**", "/ws/**").authenticated()
                        .anyRequest().denyAll()
                )
                .securityContext(context -> context.securityContextRepository(securityContexts))
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(new IncrementalAuthorizationRequestResolver(registrations, contexts)))
                        .authorizedClientRepository(authorizedClients)
                        .successHandler(success)
                        .failureHandler(new OAuth2LoginFailureHandler(identity.webOrigin()))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems.entryPoint())
                        .accessDeniedHandler(problems.accessDenied())
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .logout(logout -> logout.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}
