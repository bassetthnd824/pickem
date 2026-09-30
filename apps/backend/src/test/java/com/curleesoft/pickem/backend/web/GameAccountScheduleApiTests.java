package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.ThemeRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.AccountService;
import com.curleesoft.pickem.backend.service.PickScoring;
import com.curleesoft.pickem.backend.service.SeasonService;
import com.curleesoft.pickem.backend.service.TeamScheduleService;
import com.curleesoft.pickem.backend.service.TeamService;
import com.curleesoft.pickem.backend.service.ThemeService;
import com.curleesoft.pickem.backend.service.UserService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class GameAccountScheduleApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MatchupRepository matchupRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SeasonWeekRepository seasonWeekRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<String> userIds = new ArrayList<>();

    private final List<String> matchupIds = new ArrayList<>();

    private final List<String> teamIds = new ArrayList<>();

    private final List<String> venueIds = new ArrayList<>();

    private final List<String> weekIds = new ArrayList<>();

    private final List<String> seasonIds = new ArrayList<>();

    private final List<String> themeIds = new ArrayList<>();

    private Cookie manager;

    @BeforeEach
    void signInManager() {
        firebase.clear();
        manager = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("manager-" + UUID.randomUUID(), List.of("manager"))));
    }

    @AfterEach
    void deleteFixtures() {
        PinnedClockConfig.NOW.set(Instant.now());

        for (String id : matchupIds) {
            matchupRepository.delete(id);
        }

        for (String id : teamIds) {
            teamRepository.delete(id);
        }

        for (String id : venueIds) {
            venueRepository.delete(id);
        }

        for (String id : weekIds) {
            seasonWeekRepository.delete(id);
        }

        for (String id : seasonIds) {
            seasonRepository.delete(id);
        }

        for (String id : themeIds) {
            themeRepository.delete(id);
        }

        for (String userId : userIds) {
            userRepository.delete(userId);
        }

        matchupIds.clear();
        teamIds.clear();
        venueIds.clear();
        weekIds.clear();
        seasonIds.clear();
        themeIds.clear();
        userIds.clear();
    }

    @Test
    void scheduleAndAccountRequireASignedInPlayer() throws Exception {
        mockMvc.perform(get("/api/game/team-schedule")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/game/teams")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/game/account")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/game/account").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        Cookie stranger = player("stranger-" + token());
        mockMvc.perform(get("/api/game/account").cookie(stranger)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(UserService.NOT_FOUND));
        mockMvc.perform(get("/api/game/team-schedule").cookie(stranger).param("teamId", "missing"))
                .andExpect(status().isNotFound());

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/game/team-schedule").contains("/api/game/teams").contains("/api/game/account");
    }

    @Test
    void teamScheduleReportsResultsFromTheSelectedTeamsPerspective() throws Exception {
        Slate slate = arrange();
        Cookie player = player("schedule-" + token());

        JsonNode rows = schedule(player, slate.alabamaId(), slate.seasonId());
        assertSchedule(rows, slate);
        assertSchedule(schedule(player, slate.alabamaId(), "  " + slate.seasonId() + " "), slate);
        assertSchedule(schedule(player, slate.alabamaId(), null), slate);
        assertSchedule(schedule(player, slate.alabamaId(), "   "), slate);

        JsonNode other = schedule(player, slate.alabamaId(), slate.otherSeasonId());
        assertThat(other).hasSize(1);
        assertThat(other.get(0).path("scoreResult").asString()).isEqualTo("99 - 0");
        assertThat(other.get(0).path("opponentName").asString()).isEqualTo(slate.floridaName());

        mockMvc.perform(get("/api/game/team-schedule").cookie(player)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(TeamScheduleService.TEAM_REQUIRED));
        mockMvc.perform(get("/api/game/team-schedule").cookie(player).param("teamId", "  ").param("seasonId",
                slate.seasonId())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(TeamScheduleService.TEAM_REQUIRED));
        mockMvc.perform(get("/api/game/team-schedule").cookie(player).param("teamId", "missing-team").param("seasonId",
                slate.seasonId())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(TeamService.NOT_FOUND));
        mockMvc.perform(get("/api/game/team-schedule").cookie(player).param("teamId", slate.outsiderId())
                .param("seasonId", slate.seasonId())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(TeamScheduleService.NOT_CONFERENCE));
        mockMvc.perform(get("/api/game/team-schedule").cookie(player).param("teamId", slate.alabamaId())
                .param("seasonId", "missing-season")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(SeasonService.NOT_FOUND));

        JsonNode teams = jsonMapper.readTree(mockMvc.perform(get("/api/game/teams").cookie(player))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertConferenceTeams(teams, slate);
        JsonNode explicit = jsonMapper.readTree(mockMvc
                .perform(get("/api/game/teams").cookie(player).param("conferenceMember", "true")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertConferenceTeams(explicit, slate);
        JsonNode ignored = jsonMapper.readTree(mockMvc
                .perform(get("/api/game/teams").cookie(player).param("conferenceMember", "false")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertConferenceTeams(ignored, slate);

        Cookie asManager = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("schedule-manager-" + token(), List.of("manager"))));
        mockMvc.perform(get("/api/game/team-schedule").cookie(asManager).param("teamId", slate.alabamaId())
                .param("seasonId", slate.seasonId())).andExpect(status().isOk());
    }

    @Test
    void accountUpdatesNicknameAndThemeOnly() throws Exception {
        String uid = "acct-" + token();
        Cookie player = signIn(uid, "Ada");
        String themePath = "tide-" + token();
        createTheme("Tide " + token(), themePath, true);

        Cookie asManager = new Cookie(SessionCookies.NAME, firebase.issueCookie(identity(uid, List.of("manager"))));
        JsonNode profile = account(asManager);
        assertThat(profile.path("emailAddr").asString()).isEqualTo(uid + "@example.com");
        assertThat(profile.path("firstName").asString()).isEqualTo("Ada");
        assertThat(profile.path("lastName").asString()).isEqualTo("Player");
        assertThat(profile.path("nickName").asString()).isEqualTo("Ada");
        assertThat(profile.path("themeId").asString()).isEqualTo("light");
        assertThat(profile.path("version").asLong()).isZero();
        assertThat(profile.has("password")).isFalse();
        assertThat(profile.has("userPass")).isFalse();
        assertThat(profile.has("oldPass")).isFalse();
        assertThat(profile.has("roles")).isFalse();

        JsonNode updated = putAccount(player, """
                {"nickName":"  Tide  ","themeId":" %s ","version":0,"emailAddr":"other@example.com","firstName":"Changed","lastName":"Name","roles":["manager"]}
                """.formatted(themePath));
        assertThat(updated.path("nickName").asString()).isEqualTo("Tide");
        assertThat(updated.path("themeId").asString()).isEqualTo(themePath);
        assertThat(updated.path("emailAddr").asString()).isEqualTo(uid + "@example.com");
        assertThat(updated.path("firstName").asString()).isEqualTo("Ada");
        assertThat(updated.path("lastName").asString()).isEqualTo("Player");
        assertThat(updated.path("version").asLong()).isEqualTo(1);
        assertThat(updated.has("roles")).isFalse();

        User stored = userRepository.findById(uid).orElseThrow();
        assertThat(stored.getEmailAddr()).isEqualTo(uid + "@example.com");
        assertThat(stored.getFirstName()).isEqualTo("Ada");
        assertThat(stored.getLastName()).isEqualTo("Player");
        assertThat(stored.getNickName()).isEqualTo("Tide");
        assertThat(stored.getThemeId()).isEqualTo(themePath);
        assertThat(stored.getRoles()).containsExactly("player");
        assertThat(stored.getVersion()).isEqualTo(1L);

        rejectAccount(player, """
                {"password":"secret"}
                """, AccountService.PASSWORD_REJECTED);
        rejectAccount(player, """
                {"nickName":"Tide","themeId":"%s","version":1,"oldPass":""}
                """.formatted(themePath), AccountService.PASSWORD_REJECTED);
        rejectAccount(player, """
                {"nickName":"Tide","themeId":"%s","version":1,"userPass":"secret"}
                """.formatted(themePath), AccountService.PASSWORD_REJECTED);
        rejectAccount(player, """
                {"nickName":"Tide","themeId":"%s","version":1,"confirmPass":"secret"}
                """.formatted(themePath), AccountService.PASSWORD_REJECTED);
        rejectAccount(player, """
                {"nickName":"Tide","themeId":"%s","version":1,"UserPass":"secret"}
                """.formatted(themePath), AccountService.PASSWORD_REJECTED);
        assertThat(userRepository.findById(uid).orElseThrow().getNickName()).isEqualTo("Tide");
        assertThat(userRepository.findById(uid).orElseThrow().getVersion()).isEqualTo(1L);

        rejectAccount(player, """
                {"nickName":"   ","themeId":"%s","version":1,"user_pass":"secret"}
                """.formatted(themePath), AccountService.NICK_REQUIRED);
        rejectAccount(player, """
                {"themeId":"%s","version":1}
                """.formatted(themePath), AccountService.NICK_REQUIRED);
        rejectAccount(player, """
                {"nickName":"%s","themeId":"%s","version":1}
                """.formatted("n".repeat(41), themePath), AccountService.NICK_TOO_LONG);
        rejectAccount(player, """
                {"nickName":"Tide","version":1}
                """, AccountService.THEME_REQUIRED);
        rejectAccount(player, """
                {"nickName":"Tide","themeId":"missing-%s","version":1}
                """.formatted(token()), ThemeService.NOT_FOUND);
        rejectAccount(player, """
                {"nickName":"Tide","themeId":"%s","version":"1"}
                """.formatted(themePath), AccountService.REQUEST_INVALID);
        mockMvc.perform(put("/api/game/account").cookie(player).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(AccountService.REQUEST_INVALID));
        mockMvc.perform(put("/api/game/account").cookie(player).contentType(MediaType.APPLICATION_JSON).content("""
                {"nickName":"Tide","themeId":"%s","version":0}
                """.formatted(themePath))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Stale version")));
        mockMvc.perform(put("/api/game/account").cookie(player).contentType(MediaType.APPLICATION_JSON).content("""
                {"nickName":"Tide","themeId":"%s"}
                """.formatted(themePath))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        String maxNick = "n".repeat(AccountService.NICK_NAME_MAX);
        JsonNode maxed = putAccount(player, """
                {"nickName":"%s","themeId":"%s","version":1}
                """.formatted(maxNick, themePath));
        assertThat(maxed.path("nickName").asString()).isEqualTo(maxNick);
        assertThat(maxed.path("emailAddr").asString()).isEqualTo(uid + "@example.com");
        assertThat(maxed.path("firstName").asString()).isEqualTo("Ada");
        assertThat(maxed.path("lastName").asString()).isEqualTo("Player");
        assertThat(maxed.path("version").asLong()).isEqualTo(2);

        Cookie other = signIn("other-" + token(), "Bea");
        JsonNode otherProfile = account(other);
        assertThat(otherProfile.path("nickName").asString()).isEqualTo("Bea");
        assertThat(otherProfile.path("emailAddr").asString()).isNotEqualTo(uid + "@example.com");
        assertThat(account(player).path("nickName").asString()).isEqualTo(maxNick);
    }

    private void assertSchedule(JsonNode rows, Slate slate) {
        assertThat(rows).hasSize(5);
        assertThat(rows.get(0).path("matchupDate").asString()).isEqualTo(slate.thursday());
        assertThat(rows.get(0).path("opponentName").asString()).isEqualTo(slate.auburnName());
        assertThat(rows.get(0).path("scoreResult").asString()).isEqualTo("31 - 14");
        assertThat(rows.get(0).path("winLoss").asString()).isEqualTo("W");

        assertThat(rows.get(1).path("matchupDate").asString()).isEqualTo(slate.thursday());
        assertThat(rows.get(1).path("opponentName").asString()).isEqualTo("at " + slate.georgiaName());
        assertThat(rows.get(1).has("scoreResult")).isFalse();
        assertThat(rows.get(1).has("winLoss")).isFalse();

        assertThat(rows.get(2).path("matchupDate").asString()).isEqualTo(slate.friday());
        assertThat(rows.get(2).path("opponentName").asString()).isEqualTo(slate.floridaName());
        assertThat(rows.get(2).path("scoreResult").asString()).isEqualTo("10 - 24");
        assertThat(rows.get(2).path("winLoss").asString()).isEqualTo("L");

        assertThat(rows.get(3).path("matchupDate").asString()).isEqualTo(slate.saturday());
        assertThat(rows.get(3).path("opponentName").asString()).isEqualTo("at " + slate.outsiderName());
        assertThat(rows.get(3).path("scoreResult").asString()).isEqualTo("3 - 28");
        assertThat(rows.get(3).path("winLoss").asString()).isEqualTo("W");

        assertThat(rows.get(4).path("matchupDate").asString()).isEqualTo(slate.nextWeek());
        assertThat(rows.get(4).path("opponentName").asString()).isEqualTo("at " + slate.auburnName());
        assertThat(rows.get(4).path("scoreResult").asString()).isEqualTo("7 - 21");
        assertThat(rows.get(4).path("winLoss").asString()).isEqualTo("W");
    }

    private void assertConferenceTeams(JsonNode teams, Slate slate) {
        int alabama = indexOfName(teams, slate.alabamaName());
        int auburn = indexOfName(teams, slate.auburnName());
        int florida = indexOfName(teams, slate.floridaName());
        int georgia = indexOfName(teams, slate.georgiaName());
        assertThat(alabama).isNotNegative();
        assertThat(auburn).isNotNegative();
        assertThat(florida).isNotNegative();
        assertThat(georgia).isNotNegative();
        assertThat(alabama).isLessThan(auburn);
        assertThat(auburn).isLessThan(florida);
        assertThat(florida).isLessThan(georgia);
        assertThat(indexOfId(teams, slate.outsiderId())).isEqualTo(-1);
        assertThat(teams.get(alabama).path("squadName").asString()).isEqualTo("Crimson Tide");

        for (JsonNode team : teams) {
            assertThat(team.size()).isEqualTo(3);
            assertThat(team.hasNonNull("id")).isTrue();
            assertThat(team.hasNonNull("teamName")).isTrue();
            assertThat(team.hasNonNull("squadName")).isTrue();
            assertThat(team.has("conferenceMember")).isFalse();
            assertThat(team.has("version")).isFalse();
            assertThat(team.has("homeVenue")).isFalse();
            assertThat(team.has("homeVenueId")).isFalse();
            assertThat(team.has("cfbdTeamId")).isFalse();
        }
    }

    private Slate arrange() throws Exception {
        String token = token();
        String year = uniqueYear();
        int seasonYear = Integer.parseInt(year);
        LocalDate thursday = firstThursdayOnOrAfter(LocalDate.of(seasonYear, 9, 1));
        pin(thursday.plusDays(2));

        String venueId = createVenue("Bryant " + token, "Tuscaloosa " + token);
        String alabamaName = "Alabama " + token;
        String auburnName = "Auburn " + token;
        String georgiaName = "Georgia " + token;
        String floridaName = "Florida " + token;
        String outsiderName = "Outsider " + token;
        String alabamaId = createTeam(alabamaName, "Crimson Tide", venueId, true);
        String auburnId = createTeam(auburnName, "Tigers", venueId, true);
        String georgiaId = createTeam(georgiaName, "Bulldogs", venueId, true);
        String floridaId = createTeam(floridaName, "Gators", venueId, true);
        String outsiderId = createTeam(outsiderName, "Independents", venueId, false);

        String seasonId = createSeason(year, thursday, LocalDate.of(seasonYear, 12, 20), true);
        String weekId = createWeek(seasonId, 1, thursday, thursday.plusDays(6));
        LocalDate next = thursday.plusDays(7);
        String nextWeekId = createWeek(seasonId, 2, next, next.plusDays(6));
        createMatchup(seasonId, weekId, 1, thursday.toString(), alabamaId, auburnId, venueId, 31, 14);
        createMatchup(seasonId, weekId, 1, thursday.toString(), georgiaId, alabamaId, venueId, null, null);
        createMatchup(seasonId, weekId, 1, thursday.plusDays(1).toString(), alabamaId, floridaId, venueId, 10, 24);
        createMatchup(seasonId, weekId, 1, thursday.plusDays(1).toString(), georgiaId, floridaId, venueId, 14, 10);
        createMatchup(seasonId, weekId, 1, thursday.plusDays(2).toString(), outsiderId, alabamaId, venueId, 3, 28);
        createMatchup(seasonId, nextWeekId, 2, next.toString(), auburnId, alabamaId, venueId, 7, 21);

        int otherYear = seasonYear + 1;
        LocalDate otherThursday = firstThursdayOnOrAfter(LocalDate.of(otherYear, 9, 1));
        String otherSeasonId = createSeason(String.valueOf(otherYear), otherThursday, LocalDate.of(otherYear, 12, 20),
                false);
        String otherWeekId = createWeek(otherSeasonId, 1, otherThursday, otherThursday.plusDays(6));
        createMatchup(otherSeasonId, otherWeekId, 1, otherThursday.toString(), alabamaId, floridaId, venueId, 99, 0);

        return new Slate(seasonId, otherSeasonId, alabamaId, alabamaName, auburnName, georgiaName, floridaName,
                outsiderId, outsiderName, thursday.toString(), thursday.plusDays(1).toString(),
                thursday.plusDays(2).toString(), next.toString());
    }

    private JsonNode schedule(Cookie cookie, String teamId, String seasonId) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/game/team-schedule").cookie(cookie).param("teamId", teamId);

        if (seasonId != null) {
            request.param("seasonId", seasonId);
        }

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode account(Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/game/account").cookie(cookie)).andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode putAccount(Cookie cookie, String body) throws Exception {
        MvcResult result = mockMvc
                .perform(put("/api/game/account").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private void rejectAccount(Cookie cookie, String body, String detail) throws Exception {
        mockMvc.perform(put("/api/game/account").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(detail));
    }

    private Cookie player(String uid) {
        userIds.add(uid);
        return new Cookie(SessionCookies.NAME, firebase.issueCookie(identity(uid, List.of("player"))));
    }

    private Cookie signIn(String uid, String firstName) throws Exception {
        userIds.add(uid);
        String token = "token-" + uid;
        firebase.registerIdToken(token, new VerifiedIdentity(uid, uid + "@example.com", firstName + " Player",
                firstName, "Player", "google.com", Map.of()));
        MvcResult result = mockMvc
                .perform(post("/api/auth/session").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"%s"}
                                """.formatted(escape(token))))
                .andExpect(status().isNoContent()).andReturn();
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).isNotNull();
        int start = SessionCookies.NAME.length() + 1;
        int end = header.indexOf(';');
        return new Cookie(SessionCookies.NAME, header.substring(start, end));
    }

    private void pin(LocalDate date) {
        PinnedClockConfig.NOW.set(date.atTime(12, 0).atZone(PickScoring.LEAGUE_ZONE).toInstant());
    }

    private String createSeason(String year, LocalDate begin, LocalDate end, boolean current) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"season":"%s","beginDate":"%s","endDate":"%s","isCurrent":%s}
                        """.formatted(year, begin, end, current))).andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        seasonIds.add(id);
        return id;
    }

    private String createWeek(String seasonId, int weekNumber, LocalDate begin, LocalDate end) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seasonId":"%s","weekNumber":%d,"beginDate":"%s","endDate":"%s"}
                                """.formatted(seasonId, weekNumber, begin, end)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        weekIds.add(id);
        return id;
    }

    private String createVenue(String name, String cityState) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"venueName":"%s","cityState":"%s"}
                        """.formatted(escape(name), escape(cityState)))).andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        venueIds.add(id);
        return id;
    }

    private String createTeam(String name, String squad, String venueId, boolean conferenceMember) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"teamName":"%s","squadName":"%s","conferenceMember":%s,"homeVenueId":"%s"}
                        """.formatted(escape(name), escape(squad), conferenceMember, escape(venueId))))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        teamIds.add(id);
        return id;
    }

    private String createTheme(String name, String path, boolean active) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/themes").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"themeName":"%s","themePath":"%s","active":%s,"primary":"#9E1B32","secondary":"#FFFFFF"}
                        """.formatted(escape(name), escape(path), active))).andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        themeIds.add(id);
        return id;
    }

    private String createMatchup(String seasonId, String seasonWeekId, int weekNumber, String date, String homeTeamId,
            String awayTeamId, String venueId, Integer homeScore, Integer awayScore) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"seasonId":"%s","seasonWeekId":"%s","weekNumber":%d,"matchupDate":"%s","homeTeamId":"%s","awayTeamId":"%s","venueId":"%s","homeTeamScore":%s,"awayTeamScore":%s}
                        """.formatted(escape(seasonId), escape(seasonWeekId), weekNumber, escape(date),
                        escape(homeTeamId), escape(awayTeamId), escape(venueId), jsonNumber(homeScore),
                        jsonNumber(awayScore))))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        matchupIds.add(id);
        return id;
    }

    private String idOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
    }

    private static int indexOfName(JsonNode teams, String name) {
        for (int i = 0; i < teams.size(); i++) {
            if (name.equals(teams.get(i).path("teamName").asString())) {
                return i;
            }
        }

        return -1;
    }

    private static int indexOfId(JsonNode teams, String id) {
        for (int i = 0; i < teams.size(); i++) {
            if (id.equals(teams.get(i).path("id").asString())) {
                return i;
            }
        }

        return -1;
    }

    private static String jsonNumber(Number value) {
        return value == null ? "null" : value.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueYear() {
        return String.valueOf(8100 + ThreadLocalRandom.current().nextInt(800));
    }

    private static LocalDate firstThursdayOnOrAfter(LocalDate date) {
        LocalDate cursor = date;

        while (cursor.getDayOfWeek() != DayOfWeek.THURSDAY) {
            cursor = cursor.plusDays(1);
        }

        return cursor;
    }

    private static VerifiedIdentity identity(String uid, List<String> roles) {
        return new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test", "User", "google.com",
                RoleClaims.forRoles(roles));
    }

    private record Slate(String seasonId, String otherSeasonId, String alabamaId, String alabamaName, String auburnName,
            String georgiaName, String floridaName, String outsiderId, String outsiderName, String thursday,
            String friday, String saturday, String nextWeek) {
    }

    @TestConfiguration
    static class PinnedClockConfig {

        static final AtomicReference<Instant> NOW = new AtomicReference<>(Instant.now());

        @Bean
        @Primary
        Clock pinnedClock() {
            return new Clock() {

                @Override
                public ZoneId getZone() {
                    return PickScoring.LEAGUE_ZONE;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return Clock.fixed(instant(), zone);
                }

                @Override
                public Instant instant() {
                    return NOW.get();
                }
            };
        }
    }
}
