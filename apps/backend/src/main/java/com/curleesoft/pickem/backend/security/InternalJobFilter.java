package com.curleesoft.pickem.backend.security;

import java.io.IOException;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.curleesoft.pickem.backend.config.PickemInternalProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Admits {@code /api/internal/**} only for the shared secret or for loopback.
 * The {@code __session} cookie is not a credential and is not verified here.
 */
@Slf4j
public class InternalJobFilter extends OncePerRequestFilter {

    public static final String SECRET_HEADER = "X-Pickem-Job-Secret";

    private final PickemInternalProperties properties;

    private final JsonMapper jsonMapper;

    public InternalJobFilter(PickemInternalProperties properties, JsonMapper jsonMapper) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        return !isInternal(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!authorized(request)) {
            log.debug("Rejected internal job call");
            HttpStatus status = hasSessionCookie(request) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
            String detail = status == HttpStatus.FORBIDDEN ? "Internal jobs do not accept a session cookie"
                    : "Internal job credential is required";
            writeProblem(response, status, detail);
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new InternalJobAuthentication());
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private boolean authorized(HttpServletRequest request) {
        String header = request.getHeader(SECRET_HEADER);
        String provided = header == null ? "" : header.trim();
        if (JobSecrets.matches(provided, properties.jobSecret())) {
            return true;
        }
        String cloudRunService = System.getenv("K_SERVICE");
        if (!properties.allowsLoopback(cloudRunService == null ? "" : cloudRunService)) {
            return false;
        }
        String remoteAddress = request.getRemoteAddr();
        return LoopbackAddresses.isLoopback(remoteAddress == null ? "" : remoteAddress);
    }

    private static boolean hasSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (cookie != null && SessionCookies.NAME.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    static boolean isInternal(String uri) {
        if (uri == null) {
            return false;
        }
        return "/api/internal".equals(uri) || uri.startsWith("/api/internal/");
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        HttpStatus required = Objects.requireNonNull(status, "status");
        response.setStatus(required.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(Objects.requireNonNull(response.getWriter(), "writer"),
                ProblemDetail.forStatusAndDetail(required, detail));
    }
}
