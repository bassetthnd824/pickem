package com.curleesoft.pickem.backend.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curleesoft.pickem.backend.security.InvalidCredentialException;
import com.curleesoft.pickem.backend.security.PickemPrincipal;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.service.AuthService;

import jakarta.validation.Valid;

/**
 * Session login for the Next.js BFF. The browser never calls Spring; the BFF
 * forwards the ID token and copies {@code Set-Cookie}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final SessionCookies sessionCookies;

    public AuthController(AuthService authService, SessionCookies sessionCookies) {
        this.authService = authService;
        this.sessionCookies = sessionCookies;
    }

    @PostMapping("/session")
    public ResponseEntity<Void> createSession(@Valid @RequestBody SessionRequest request) {
        String sessionCookie = authService.establishSession(request.idToken());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sessionCookies.issue(sessionCookie).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout(principal(authentication));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sessionCookies.clear().toString()).build();
    }

    @GetMapping("/me")
    public AuthProfileResponse me(Authentication authentication) {
        return authService.profile(principal(authentication));
    }

    private static PickemPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PickemPrincipal principal) {
            return principal;
        }
        throw new InvalidCredentialException("Authentication is required");
    }
}
