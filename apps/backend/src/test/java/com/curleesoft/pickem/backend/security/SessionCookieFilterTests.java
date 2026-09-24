package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.curleesoft.pickem.backend.config.PickemAuthProperties;
import com.curleesoft.pickem.backend.repository.FirestoreAccessException;
import com.curleesoft.pickem.backend.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

class SessionCookieFilterTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SessionCookies sessionCookies = new SessionCookies(
            new PickemAuthProperties("fake", Duration.ofDays(14), ""));

    @Test
    void missingAdminCredentialsWithASessionCookieReturns503() throws Exception {
        FirebaseIdentityClient identities = new StubIdentityClient(new AuthUnavailableException(
                "Firebase Admin credentials are not configured", new IOException("adc")));
        SessionCookieFilter filter = filter(identities, mock(UserRepository.class));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithCookie(), response, (servletRequest, servletResponse) -> {
            throw new AssertionError("filter chain continued");
        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("Firebase Admin credentials are not configured");
    }

    @Test
    void firestoreFailureWhileResolvingRolesReturns503() throws Exception {
        UserRepository users = mock(UserRepository.class);
        when(users.findById("uid-1")).thenThrow(new FirestoreAccessException("Failed to read users/uid-1", null));
        SessionCookieFilter filter = filter(new StubIdentityClient(null), users);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithCookie(), response, (servletRequest, servletResponse) -> {
            throw new AssertionError("filter chain continued");
        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("Authentication is temporarily unavailable");
        assertThat(response.getContentAsString()).doesNotContain("users/uid-1");
    }

    @Test
    void invalidCookieContinuesUnauthenticated() throws Exception {
        FirebaseIdentityClient identities = new StubIdentityClient(new InvalidCredentialException("Invalid or revoked session cookie"));
        SessionCookieFilter filter = filter(identities, mock(UserRepository.class));
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(requestWithCookie(), new MockHttpServletResponse(), (servletRequest, servletResponse) -> continued.set(true));

        assertThat(continued).isTrue();
    }

    private SessionCookieFilter filter(FirebaseIdentityClient identities, UserRepository users) {
        return new SessionCookieFilter(identities, sessionCookies, users, jsonMapper);
    }

    private static MockHttpServletRequest requestWithCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SessionCookies.NAME, "session-cookie"));
        return request;
    }

    private static final class StubIdentityClient implements FirebaseIdentityClient {

        private final RuntimeException sessionFailure;

        private StubIdentityClient(RuntimeException sessionFailure) {
            this.sessionFailure = sessionFailure;
        }

        @Override
        public VerifiedIdentity verifyIdToken(String idToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String createSessionCookie(String idToken, Duration expiresIn) {
            throw new UnsupportedOperationException();
        }

        @Override
        public VerifiedIdentity verifySessionCookie(String sessionCookie) {
            if (sessionFailure != null) {
                throw sessionFailure;
            }
            return new VerifiedIdentity("uid-1", "a@b.com", "A B", "A", "B", "google.com", Map.of());
        }

        @Override
        public void setCustomUserClaims(String uid, Map<String, Object> claims) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revokeRefreshTokens(String uid) {
            throw new UnsupportedOperationException();
        }
    }
}
