package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionTests extends FirestoreEmulatorSupport {

    private static final long FOURTEEN_DAYS_SECONDS = 14L * 24 * 60 * 60;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void resetFirebase() {
        firebase.clear();
    }

    @Test
    void firstGoogleSignInProvisionsPlayerAndSetsHostOnlySessionCookie() throws Exception {
        String uid = uid();
        registerGoogle(uid, "token-1", "kenney@example.com", "Kenney", "Curlee");

        MvcResult result = mockMvc
                .perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "http://localhost:3000").content("""
                                {"idToken":"token-1","password":"secret","userPass":"hash"}
                                """))
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().string(headerName(), hostOnlySessionCookie())).andReturn();

        User user = userRepository.findById(uid).orElseThrow();
        assertThat(user.getUid()).isEqualTo(uid);
        assertThat(user.getId()).isEqualTo(uid);
        assertThat(user.getEmailAddr()).isEqualTo("kenney@example.com");
        assertThat(user.getFirstName()).isEqualTo("Kenney");
        assertThat(user.getLastName()).isEqualTo("Curlee");
        assertThat(user.getNickName()).isEqualTo("Kenney");
        assertThat(user.getThemeId()).isEqualTo("light");
        assertThat(user.getRoles()).containsExactly("player");
        assertThat(jsonMapper.writeValueAsString(user)).doesNotContain("password");
        assertThat(firebase.claimWriteCount(uid)).isEqualTo(1);
        assertThat(firebase.customClaims(uid)).containsEntry("player", true);
        assertThat(RoleClaims.read(firebase.customClaims(uid))).containsExactly("player");

        String sessionCookie = sessionCookie(result);
        assertThat(RoleClaims.read(firebase.identityForCookie(sessionCookie).orElseThrow().claims())).isEmpty();

        mockMvc.perform(get("/api/auth/me").cookie(session(sessionCookie))).andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(uid)).andExpect(jsonPath("$.emailAddr").value("kenney@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("player")).andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/game/main").cookie(session(sessionCookie))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/manager/seasons").cookie(session(sessionCookie))).andExpect(status().isForbidden());
    }

    @Test
    void repeatSignInRefreshesGoogleProfileAndKeepsNickNameAndTheme() throws Exception {
        String uid = uid();
        registerGoogle(uid, "token-1", "old@example.com", "Old", "Name");
        mockMvc.perform(
                post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token-1\"}"))
                .andExpect(status().isNoContent());

        User existing = userRepository.findById(uid).orElseThrow();
        existing.setNickName("KC");
        existing.setThemeId("alabama");
        userRepository.save(existing, uid);

        registerGoogle(uid, "token-2", "new@example.com", "New", "Person");
        mockMvc.perform(
                post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token-2\"}"))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(uid).orElseThrow();
        assertThat(updated.getEmailAddr()).isEqualTo("new@example.com");
        assertThat(updated.getFirstName()).isEqualTo("New");
        assertThat(updated.getLastName()).isEqualTo("Person");
        assertThat(updated.getNickName()).isEqualTo("KC");
        assertThat(updated.getThemeId()).isEqualTo("alabama");
        assertThat(updated.getRoles()).containsExactly("player");
        assertThat(firebase.claimWriteCount(uid)).isEqualTo(1);
        assertThat(userRepository.findAll().stream().filter(user -> uid.equals(user.getUid())).toList()).hasSize(1);
    }

    @Test
    void unauthenticatedGameAndManagerCallsAreRejected() throws Exception {
        mockMvc.perform(get("/api/game/main")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/seasons")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void playerIsBlockedFromManagerAndManagerMayCallBoth() throws Exception {
        String playerCookie = firebase.issueCookie(signedIn("player-1", List.of("player")));
        mockMvc.perform(get("/api/game/main").cookie(session(playerCookie))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/manager/seasons").cookie(session(playerCookie))).andExpect(status().isForbidden());

        String managerCookie = firebase.issueCookie(signedIn("manager-1", List.of("manager")));
        mockMvc.perform(get("/api/game/main").cookie(session(managerCookie))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/manager/seasons").cookie(session(managerCookie))).andExpect(status().isOk());
    }

    @Test
    void logoutClearsTheCookieAndARevokedSessionIsRejected() throws Exception {
        String uid = uid();
        registerGoogle(uid, "token-1", "kenney@example.com", "Kenney", "Curlee");
        MvcResult login = mockMvc.perform(
                post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token-1\"}"))
                .andExpect(status().isNoContent()).andReturn();
        String sessionCookie = sessionCookie(login);

        mockMvc.perform(post("/api/auth/logout").cookie(session(sessionCookie))).andExpect(status().isNoContent())
                .andExpect(header().string(headerName(), clearedSessionCookie()));

        mockMvc.perform(get("/api/game/main").cookie(session(sessionCookie))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").cookie(session(sessionCookie))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/").cookie(session(sessionCookie))).andExpect(status().isOk());
    }

    @Test
    void logoutRejectsTheSameIdTokenUntilANewGoogleSignIn() throws Exception {
        String uid = uid();
        registerGoogle(uid, "token-1", "kenney@example.com", "Kenney", "Curlee");
        MvcResult login = mockMvc.perform(
                post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token-1\"}"))
                .andExpect(status().isNoContent()).andReturn();

        mockMvc.perform(post("/api/auth/logout").cookie(session(sessionCookie(login)))).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token-1\"}"))
                .andExpect(status().isUnauthorized());

        registerGoogle(uid, "token-2", "kenney@example.com", "Kenney", "Curlee");
        mockMvc.perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token-2\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidTokenMissingTokenAndRegistrationAreRejected() throws Exception {
        mockMvc.perform(
                post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"missing\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        registerGoogle(uid(), "password-token", "a@b.com", "A", "B", "password");
        mockMvc.perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"password-token\"}")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailAddr\":\"a@b.com\",\"password\":\"secret\"}")).andExpect(status().isNotFound());
    }

    @Test
    void openApiDocumentsAuthWithoutPasswordsOrRegistration() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();

        assertThat(spec).contains("/api/auth/session");
        assertThat(spec).contains("/api/auth/logout");
        assertThat(spec).contains("/api/auth/me");
        assertThat(spec).doesNotContain("/api/auth/register");
        assertThat(spec).doesNotContain("password");
    }

    private void registerGoogle(String uid, String idToken, String email, String firstName, String lastName) {
        registerGoogle(uid, idToken, email, firstName, lastName, "google.com");
    }

    private void registerGoogle(String uid, String idToken, String email, String firstName, String lastName,
            String provider) {
        firebase.registerIdToken(idToken,
                new VerifiedIdentity(uid, email, firstName + " " + lastName, firstName, lastName, provider, Map.of()));
    }

    private static VerifiedIdentity signedIn(String uid, List<String> roles) {
        return new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test", "User", "google.com",
                RoleClaims.forRoles(roles));
    }

    private static Cookie session(String value) {
        return new Cookie(SessionCookies.NAME, value);
    }

    private static String sessionCookie(MvcResult result) {
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).isNotNull();
        int start = SessionCookies.NAME.length() + 1;
        int end = header.indexOf(';');
        return header.substring(start, end);
    }

    private static org.hamcrest.Matcher<String> hostOnlySessionCookie() {
        return org.hamcrest.Matchers.allOf(containsString(SessionCookies.NAME + "="), containsString("Path=/"),
                containsString("Max-Age=" + FOURTEEN_DAYS_SECONDS), containsString("Secure"),
                containsString("HttpOnly"), containsString("SameSite=Lax"), not(containsString("Domain=")));
    }

    private static org.hamcrest.Matcher<String> clearedSessionCookie() {
        return org.hamcrest.Matchers.allOf(containsString(SessionCookies.NAME + "="), containsString("Max-Age=0"),
                containsString("Path=/"), containsString("Secure"), containsString("HttpOnly"),
                containsString("SameSite=Lax"), not(containsString("Domain=")));
    }

    private static String headerName() {
        return "Set-Cookie";
    }

    private static String uid() {
        return "uid-" + UUID.randomUUID();
    }
}
