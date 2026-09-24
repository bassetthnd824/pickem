package com.curleesoft.pickem.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.curleesoft.pickem.backend.repository.FirestoreAccessException;
import com.curleesoft.pickem.backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies the forwarded {@code __session} cookie with revocation checking and
 * loads roles from its custom claims.
 */
public class SessionCookieFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionCookieFilter.class);

    private final FirebaseIdentityClient identityClient;

    private final SessionCookies sessionCookies;

    private final UserRepository userRepository;

    private final JsonMapper jsonMapper;

    public SessionCookieFilter(FirebaseIdentityClient identityClient, SessionCookies sessionCookies,
            UserRepository userRepository, JsonMapper jsonMapper) {
        this.identityClient = identityClient;
        this.sessionCookies = sessionCookies;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> cookie = sessionCookies.read(request);
        if (cookie.isPresent()) {
            try {
                authenticate(cookie.get());
            } catch (AuthUnavailableException ex) {
                // Controller advice does not run for a filter.
                log.warn("Firebase Admin credentials are not available");
                writeProblem(response, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
                return;
            } catch (FirestoreAccessException ex) {
                log.warn("Could not resolve roles for a session cookie", ex);
                writeProblem(response, HttpStatus.SERVICE_UNAVAILABLE, "Authentication is temporarily unavailable");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String sessionCookie) {
        try {
            VerifiedIdentity identity = identityClient.verifySessionCookie(sessionCookie);
            List<String> roles = rolesFor(identity);
            PickemPrincipal principal = new PickemPrincipal(identity.uid(), roles);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new SessionAuthentication(principal, RoleClaims.authorities(roles)));
            SecurityContextHolder.setContext(context);
        } catch (InvalidCredentialException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Rejected session cookie");
        }
    }

    /**
     * Session cookies carry the ID token's claims. The {@code player} claim is
     * written on first sign-in, after that ID token was issued, so the first cookie
     * has no role claims. Fall back to the {@code users} document, which was
     * written in the same sign-in. A cookie that already has role claims keeps
     * those claims until the next sign-in mints a new cookie.
     */
    private List<String> rolesFor(VerifiedIdentity identity) {
        List<String> roles = RoleClaims.read(identity.claims());
        if (!roles.isEmpty()) {
            return roles;
        }
        return userRepository.findById(identity.uid()).map(user -> user.getRoles()).map(RoleClaims::normalize)
                .orElse(List.of());
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getWriter(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
