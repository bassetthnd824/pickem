package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.InternalJobFilter;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class InternalJobApiTests extends FirestoreEmulatorSupport {

    private static final String SECRET = "test-secret";

    private static final String OUTSIDE = "203.0.113.10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    /**
     * Auth tests must not call College Football Data when the shared emulator
     * already holds a current-season game for today.
     */
    @MockitoBean
    private CfbdClient cfbdClient;

    @DynamicPropertySource
    static void jobSecret(DynamicPropertyRegistry registry) {
        registry.add("pickem.internal.job-secret", () -> SECRET);
    }

    @BeforeEach
    void resetFirebase() {
        firebase.clear();
        lenient().when(cfbdClient.games(anyInt(), isNull(), any())).thenReturn(List.of());
    }

    @Test
    void sessionCookieAloneDoesNotAdmitTheScoreJob() throws Exception {
        String session = signIn();

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").cookie(sessionCookie(session)).with(from(OUTSIDE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Internal jobs do not accept a session cookie"))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void missingCredentialIsUnauthorizedAndAWrongSecretDoesNotEcho() throws Exception {
        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(from(OUTSIDE))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Internal job credential is required"));

        mockMvc.perform(post("/api/internal/jobs/cfbd-matchups").header(InternalJobFilter.SECRET_HEADER, "nope")
                .with(from(OUTSIDE))).andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("nope"))));

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").cookie(sessionCookie("only-a-cookie"))
                .header("X-Forwarded-For", "127.0.0.1").with(from(OUTSIDE))).andExpect(status().isForbidden());
    }

    @Test
    void sharedSecretAdmitsBothJobsFromOutsideLoopback() throws Exception {
        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").header(InternalJobFilter.SECRET_HEADER, SECRET)
                .cookie(sessionCookie("ignored")).with(from(OUTSIDE))).andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("cfbd-scores"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(post("/api/internal/jobs/cfbd-matchups").header(InternalJobFilter.SECRET_HEADER, "  " + SECRET)
                .with(from(OUTSIDE))).andExpect(status().isAccepted()).andExpect(jsonPath("$.job").value("cfbd-matchups"));
    }

    @Test
    void loopbackAdmitsTheJobWithoutASecretOrIdentityToken() throws Exception {
        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(from("127.0.0.1"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("cfbd-scores"));

        mockMvc.perform(post("/api/internal/jobs/cfbd-matchups").with(from("::1"))).andExpect(status().isAccepted());
    }

    @Test
    void secretDoesNotOpenOtherMethodsOrUnknownJobs() throws Exception {
        mockMvc.perform(get("/api/internal/jobs/cfbd-scores").header(InternalJobFilter.SECRET_HEADER, SECRET)
                .with(from(OUTSIDE))).andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/api/internal/jobs/other").header(InternalJobFilter.SECRET_HEADER, SECRET).with(from(OUTSIDE)))
                .andExpect(status().isNotFound());
    }

    private String signIn() throws Exception {
        String uid = "uid-" + UUID.randomUUID();
        firebase.registerIdToken("token-" + uid, new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test",
                "User", "google.com", Map.of()));
        MvcResult result = mockMvc
                .perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token-" + uid + "\"}"))
                .andExpect(status().isNoContent()).andReturn();
        String header = Objects.requireNonNull(result.getResponse().getHeader("Set-Cookie"), "Set-Cookie");
        int start = SessionCookies.NAME.length() + 1;
        int end = header.indexOf(';');
        assertThat(end).isGreaterThan(start);
        return header.substring(start, end);
    }

    private static Cookie sessionCookie(String value) {
        return new Cookie(SessionCookies.NAME, value);
    }

    private static RequestPostProcessor from(String remoteAddress) {
        String address = Objects.requireNonNull(remoteAddress, "remoteAddress");
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
