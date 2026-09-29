package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.curleesoft.pickem.backend.repository.ThemeRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.ThemeService;
import com.curleesoft.pickem.backend.service.UserService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ManagerUserThemeApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private final List<String> userIds = new ArrayList<>();

    private final List<String> themeIds = new ArrayList<>();

    private String managerUid;

    private Cookie manager;

    @BeforeEach
    void signInManager() {
        firebase.clear();
        managerUid = "manager-" + UUID.randomUUID();
        manager = new Cookie(SessionCookies.NAME, firebase.issueCookie(identity(managerUid, List.of("manager"))));
    }

    @AfterEach
    void deleteFixtures() {
        for (String id : userIds) {
            userRepository.delete(id);
        }
        for (String id : themeIds) {
            themeRepository.delete(id);
        }
        userIds.clear();
        themeIds.clear();
    }

    @Test
    void usersAndThemesRequireTheManagerRole() throws Exception {
        Cookie player = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("player-" + UUID.randomUUID(), List.of("player"))));

        mockMvc.perform(get("/api/manager/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/manager/users/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/manager/users/missing")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/themes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/themes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/manager/themes/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/manager/themes/missing")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/manager/users").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/users/missing").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/themes").cookie(player).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/manager/themes/missing").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/manager/users/missing").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/themes").cookie(player)).andExpect(status().isForbidden());
    }

    @Test
    void themesKeepAUniqueNameAndKeyAndTheActiveFlag() throws Exception {
        String token = token();
        String lightKey = "light-" + token;
        String darkKey = "dark-" + token;

        MvcResult created = mockMvc
                .perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(themeBody("Light " + token, lightKey, true, "#1F2937", "#2563EB", null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/manager/themes/")))
                .andExpect(jsonPath("$.themeName").value("Light " + token))
                .andExpect(jsonPath("$.themePath").value(lightKey)).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.primary").value("#1F2937")).andExpect(jsonPath("$.secondary").value("#2563EB"))
                .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.lastUpdateUser").value(managerUid))
                .andExpect(jsonPath("$.createDate").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z"))).andReturn();

        String lightId = idOf(created);
        themeIds.add(lightId);
        String darkId = createTheme("Dark " + token, darkKey, false);

        mockMvc.perform(get("/api/manager/themes/" + lightId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("Light " + token, "other-" + token, true, "#000000", "#FFFFFF", null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(ThemeService.NAME_NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("Other " + token, lightKey, true, "#000000", "#FFFFFF", null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(ThemeService.PATH_NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody(" ", lightKey, true, "#000000", "#FFFFFF", null))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("themeName")));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("x".repeat(41), "long-" + token, true, "#000000", "#FFFFFF", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("themeName")));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("Slash " + token, "/" + lightKey, true, "#000000", "#FFFFFF", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(ThemeService.PATH_SLASH));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("Long " + token, "k".repeat(101), true, "#000000", "#FFFFFF", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("themePath")));

        mockMvc.perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                {"themeName":"No active %s","themePath":"no-active-%s","primary":"#000000","secondary":"#FFFFFF"}
                """.formatted(token, token))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("active")));

        mockMvc.perform(get("/api/manager/themes").cookie(manager).param("themeName", "light " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(lightId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/themes").cookie(manager).param("themePath", token).param("active", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(darkId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        String listed = mockMvc.perform(get("/api/manager/themes").cookie(manager).param("themePath", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode themes = jsonMapper.readTree(listed);
        assertThat(indexOf(themes, darkId)).isLessThan(indexOf(themes, lightId));

        mockMvc.perform(put("/api/manager/themes/" + lightId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("Light " + token, lightKey, false, "#1F2937", "#2563EB", 4L)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        mockMvc.perform(put("/api/manager/themes/" + lightId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(themeBody("Day " + token, lightKey, false, "#111111", "#EEEEEE", 0L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.themeName").value("Day " + token))
                .andExpect(jsonPath("$.primary").value("#111111"))
                .andExpect(jsonPath("$.createUser").value(managerUid));

        mockMvc.perform(get("/api/manager/themes/missing-" + token).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(ThemeService.NOT_FOUND));

        mockMvc.perform(delete("/api/manager/themes/" + darkId).cookie(manager)).andExpect(status().isNoContent());
        themeIds.remove(darkId);
        mockMvc.perform(get("/api/manager/themes/" + darkId).cookie(manager)).andExpect(status().isNotFound());
    }

    @Test
    void usersSearchByNameAndMirrorManagerClaims() throws Exception {
        String token = token();
        String themeKey = "user-theme-" + token;
        createTheme("Players " + token, themeKey, true);

        String adaUid = "ada-" + token;
        String graceUid = "grace-" + token;
        MvcResult created = mockMvc
                .perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(userBody(adaUid, "ada-" + token + "@example.com", "Ada", "Lovelace", "Ada", themeKey,
                                "[\"player\"]", null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/manager/users/" + adaUid)))
                .andExpect(jsonPath("$.uid").value(adaUid)).andExpect(jsonPath("$.id").value(adaUid))
                .andExpect(jsonPath("$.roles[0]").value("player")).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.themeId").value(themeKey)).andReturn();

        String body = created.getResponse().getContentAsString();
        assertThat(body).doesNotContain("userPass");
        assertThat(body).doesNotContain("password");
        userIds.add(adaUid);

        Map<String, Object> playerClaims = firebase.customClaims(adaUid);
        assertThat(playerClaims.get(RoleClaims.PLAYER)).isEqualTo(Boolean.TRUE);
        assertThat(playerClaims).doesNotContainKey(RoleClaims.MANAGER);

        String graceId = createUser(graceUid, "grace-" + token + "@example.com", "Grace", "Hopper", themeKey,
                "[\"player\"]");

        mockMvc.perform(get("/api/manager/users/" + adaUid).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nickName").value("Ada")).andExpect(jsonPath("$.lastName").value("Lovelace"));

        mockMvc.perform(get("/api/manager/users").cookie(manager).param("emailAddr", "ADA-" + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(adaUid))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/users").cookie(manager).param("firstName", "grace").param("lastName", "hop"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(graceId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        String listed = mockMvc.perform(get("/api/manager/users").cookie(manager).param("emailAddr", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode users = jsonMapper.readTree(listed);
        assertThat(indexOf(users, adaUid)).isLessThan(indexOf(users, graceId));

        mockMvc.perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody("other-" + token, "ada-" + token + "@example.com", "Ada", "Other", "Ada", themeKey,
                        "[\"player\"]", null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(UserService.EMAIL_NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody(adaUid, "fresh-" + token + "@example.com", "Ada", "Lovelace", "Ada", themeKey,
                        "[\"player\"]", null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(UserService.ALREADY_EXISTS));

        mockMvc.perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody("empty-" + token, "empty-" + token + "@example.com", "Empty", "Roles", "Empty",
                        themeKey, "[]", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(UserService.ROLES_REQUIRED));

        mockMvc.perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody("coach-" + token, "coach-" + token + "@example.com", "Coach", "Role", "Coach",
                        themeKey, "[\"coach\"]", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(UserService.ROLE_INVALID));

        mockMvc.perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody("missing-" + token, "missing-" + token + "@example.com", "Missing", "Theme", "Miss",
                        "not-a-theme-" + token, "[\"player\"]", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(ThemeService.NOT_FOUND));

        mockMvc.perform(put("/api/manager/users/" + adaUid).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody(adaUid, "ada-" + token + "@example.com", "Ada", "Lovelace", "Countess", themeKey,
                        "[\"player\",\"manager\"]", 4L)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("Stale version")));
        assertThat(firebase.customClaims(adaUid)).doesNotContainKey(RoleClaims.MANAGER);

        mockMvc.perform(put("/api/manager/users/" + adaUid).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody(adaUid, "ada-" + token + "@example.com", "Ada", "Lovelace", "Countess", themeKey,
                        "[\"player\",\"manager\"]", 0L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.roles[0]").value("player")).andExpect(jsonPath("$.roles[1]").value("manager"))
                .andExpect(jsonPath("$.nickName").value("Countess"));

        Map<String, Object> elevated = firebase.customClaims(adaUid);
        assertThat(elevated.get(RoleClaims.MANAGER)).isEqualTo(Boolean.TRUE);
        assertThat(elevated.get(RoleClaims.PLAYER)).isEqualTo(Boolean.TRUE);
        assertThat(elevated.get(RoleClaims.ROLES)).isEqualTo(List.of("player", "manager"));

        mockMvc.perform(put("/api/manager/users/" + adaUid).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody(adaUid, "ada-" + token + "@example.com", "Ada", "Lovelace", "Countess", themeKey,
                        "[]", 1L)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(UserService.ROLES_REQUIRED));
        assertThat(firebase.customClaims(adaUid).get(RoleClaims.MANAGER)).isEqualTo(Boolean.TRUE);

        mockMvc.perform(put("/api/manager/users/" + adaUid).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(userBody(adaUid, "ada-" + token + "@example.com", "Ada", "Lovelace", "Countess", themeKey,
                        "[\"player\"]", 1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("player"))
                .andExpect(jsonPath("$.roles[1]").doesNotExist());

        Map<String, Object> cleared = firebase.customClaims(adaUid);
        assertThat(cleared).doesNotContainKey(RoleClaims.MANAGER);
        assertThat(cleared.get(RoleClaims.PLAYER)).isEqualTo(Boolean.TRUE);
        assertThat(cleared.get(RoleClaims.ROLES)).isEqualTo(List.of("player"));

        mockMvc.perform(post("/api/manager/users/" + adaUid + "/password").cookie(manager)
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"secret\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/manager/users/" + adaUid + "/reset-password").cookie(manager)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/manager/users/missing-" + token).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(UserService.NOT_FOUND));

        mockMvc.perform(delete("/api/manager/users/" + graceId).cookie(manager)).andExpect(status().isNoContent());
        userIds.remove(graceId);
        mockMvc.perform(get("/api/manager/users/" + graceId).cookie(manager)).andExpect(status().isNotFound());

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/manager/users");
        assertThat(spec).contains("/api/manager/themes");
        assertThat(spec).doesNotContain("userPass");
        assertThat(spec).doesNotContain("/password");
        assertThat(spec).doesNotContain("reset-password");
    }

    private String createTheme(String name, String path, boolean active) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(themeBody(name, path, active, "#000000", "#FFFFFF", null)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        themeIds.add(id);
        return id;
    }

    private String createUser(String uid, String email, String firstName, String lastName, String themeId,
            String rolesJson) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/users").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(userBody(uid, email, firstName, lastName, firstName, themeId, rolesJson, null)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        userIds.add(id);
        return id;
    }

    private String idOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
    }

    private static int indexOf(JsonNode documents, String id) {
        for (int i = 0; i < documents.size(); i++) {
            if (id.equals(documents.get(i).path("id").asString())) {
                return i;
            }
        }
        return -1;
    }

    private static String themeBody(String name, String path, boolean active, String primary, String secondary,
            Long version) {
        String versionField = version == null ? "" : ",\"version\":" + version;
        return """
                {"themeName":"%s","themePath":"%s","active":%s,"primary":"%s","secondary":"%s","createUser":"hacker"%s}
                """.formatted(escape(name), escape(path), active, escape(primary), escape(secondary), versionField);
    }

    private static String userBody(String uid, String email, String firstName, String lastName, String nickName,
            String themeId, String rolesJson, Long version) {
        String versionField = version == null ? "" : ",\"version\":" + version;
        return """
                {"uid":"%s","emailAddr":"%s","firstName":"%s","lastName":"%s","nickName":"%s","themeId":"%s","roles":%s,"userPass":"secret","password":"secret","createUser":"hacker"%s}
                """.formatted(escape(uid), escape(email), escape(firstName), escape(lastName), escape(nickName),
                        escape(themeId), rolesJson, versionField);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static VerifiedIdentity identity(String uid, List<String> roles) {
        return new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test", "User", "google.com",
                RoleClaims.forRoles(roles));
    }
}
