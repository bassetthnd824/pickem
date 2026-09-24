package com.curleesoft.pickem.backend.config;

import java.io.IOException;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.SessionCookieFilter;
import com.curleesoft.pickem.backend.security.SessionCookies;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring is not browser-facing: the Next.js BFF is the only caller, so CORS is
 * disabled and browser CSRF against this API is N/A. The BFF forwards the
 * host-only {@code __session} cookie; {@link SessionCookieFilter} verifies it.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SessionCookieFilter sessionCookieFilter(FirebaseIdentityClient identityClient, SessionCookies sessionCookies,
            UserRepository userRepository, JsonMapper jsonMapper) {
        return new SessionCookieFilter(identityClient, sessionCookies, userRepository, jsonMapper);
    }

    /**
     * Registering a {@link jakarta.servlet.Filter} bean also installs it on the
     * servlet container. It must run only inside the security chain, after the
     * security context exists.
     */
    @Bean
    public FilterRegistrationBean<SessionCookieFilter> sessionCookieFilterRegistration(
            SessionCookieFilter sessionCookieFilter) {
        FilterRegistrationBean<SessionCookieFilter> registration = new FilterRegistrationBean<>(sessionCookieFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionCookieFilter sessionCookieFilter,
            JsonMapper jsonMapper) throws Exception {
        http.cors(cors -> cors.disable()).csrf(csrf -> csrf.disable()).httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable()).logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/auth/session").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/manager/**").hasRole("MANAGER").requestMatchers("/api/game/**")
                        .hasAnyRole("PLAYER", "MANAGER").anyRequest().permitAll())
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint((request, response, authException) -> writeProblem(jsonMapper,
                                response, HttpStatus.UNAUTHORIZED, "Authentication is required")).accessDeniedHandler(
                                        (request, response, accessDeniedException) -> writeProblem(jsonMapper, response,
                                                HttpStatus.FORBIDDEN, "Manager role is required")))
                .addFilterBefore(sessionCookieFilter, AnonymousAuthenticationFilter.class);
        return http.build();
    }

    private static void writeProblem(JsonMapper jsonMapper, HttpServletResponse response, HttpStatus status,
            String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getWriter(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
