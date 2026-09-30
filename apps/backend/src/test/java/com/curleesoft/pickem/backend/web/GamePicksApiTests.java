package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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

import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.PickScoring;
import com.curleesoft.pickem.backend.service.PickService;
import com.curleesoft.pickem.backend.service.SeasonWeekService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class GamePicksApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private PickRepository pickRepository;

    @Autowired
    private MatchupRepository matchupRepository;

    @Autowired
    private RivalryRepository rivalryRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SeasonWeekRepository seasonWeekRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    private final List<String> userIds = new ArrayList<>();

    private final List<String> matchupIds = new ArrayList<>();

    private final List<String> rivalryIds = new ArrayList<>();

    private final List<String> teamIds = new ArrayList<>();

    private final List<String> venueIds = new ArrayList<>();

    private final List<String> weekIds = new ArrayList<>();

    private final List<String> seasonIds = new ArrayList<>();

    private Cookie manager;

    @BeforeEach
    void signInManager() {
        firebase.clear();
        manager = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("manager-" + UUID.randomUUID(), List.of("manager"))));
    }

    @AfterEach
    void deleteFixtures() {
        for (String userId : userIds) {
            for (Pick pick : pickRepository.findByUserId(userId)) {
                pickRepository.delete(pick.getId());
            }
        }

        for (String id : matchupIds) {
            matchupRepository.delete(id);
        }

        for (String id : rivalryIds) {
            rivalryRepository.delete(id);
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

        userIds.clear();
        matchupIds.clear();
        rivalryIds.clear();
        teamIds.clear();
        venueIds.clear();
        weekIds.clear();
        seasonIds.clear();
    }

    @Test
    void gameRoutesRequireASignedInPlayer() throws Exception {
        Cookie player = player("player-" + UUID.randomUUID());

        mockMvc.perform(get("/api/game/main")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/game/picks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        int mainStatus = mockMvc.perform(get("/api/game/main").cookie(player)).andReturn().getResponse().getStatus();
        assertThat(mainStatus).isIn(200, 404);
        mockMvc.perform(post("/api/game/picks").cookie(player).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        int managerStatus = mockMvc.perform(get("/api/game/main").cookie(manager)).andReturn().getResponse().getStatus();
        assertThat(managerStatus).isIn(200, 404);

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/game/main").contains("/api/game/picks");
    }

    @Test
    void mainJoinsPicksAndHidesFutureResults() throws Exception {
        Slate slate = arrange();
        Cookie player = player("player-" + UUID.randomUUID());

        mockMvc.perform(post("/api/game/picks").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content(picksBody(slate.currentWeekId(),
                        pickRow(slate.correctId(), slate.correctHomeId(), 5) + ","
                                + pickRow(slate.wrongId(), slate.wrongHomeId(), 1) + ","
                                + pickRow(slate.alphaId(), null, null))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/game/picks").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content(picksBody(slate.futureWeekId(), pickRow(slate.futureId(), slate.futureAwayId(), 2))))
                .andExpect(status().isOk());

        JsonNode main = jsonMapper.readTree(mockMvc.perform(get("/api/game/main").cookie(player)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(main.path("seasonId").asString()).isEqualTo(slate.seasonId());
        assertThat(main.path("season").asString()).isEqualTo(slate.year());
        assertThat(main.path("weeks")).hasSize(2);
        assertThat(main.path("weeks").get(0).path("id").asString()).isEqualTo(slate.currentWeekId());
        assertThat(main.path("weeks").get(0).path("currentOrPast").asBoolean()).isTrue();
        assertThat(main.path("weeks").get(1).path("id").asString()).isEqualTo(slate.futureWeekId());
        assertThat(main.path("weeks").get(1).path("currentOrPast").asBoolean()).isFalse();

        long conference = teamRepository.findAll().stream().filter(team -> Boolean.TRUE.equals(team.getConferenceMember()))
                .count();
        assertThat(main.path("numberOfConferenceTeams").asInt()).isEqualTo((int) conference);
        assertThat(conference).isLessThan(teamRepository.findAll().size());

        JsonNode current = main.path("weeks").get(0).path("matchups");
        assertThat(indexOf(current, slate.correctId())).isLessThan(indexOf(current, slate.wrongId()));
        assertThat(indexOf(current, slate.wrongId())).isLessThan(indexOf(current, slate.alphaId()));
        assertThat(indexOf(current, slate.alphaId())).isLessThan(indexOf(current, slate.zuluId()));

        JsonNode correct = matchup(current, slate.correctId());
        assertThat(correct.path("homeTeam").path("name").asString()).isEqualTo(slate.correctHomeName());
        assertThat(correct.path("homeTeam").path("squad").asString()).isEqualTo("Crimson Tide");
        assertThat(correct.path("awayTeam").path("name").asString()).isEqualTo(slate.correctAwayName());
        assertThat(correct.path("awayTeam").path("squad").asString()).isEqualTo("Tigers");
        assertThat(correct.path("venue").path("name").asString()).isEqualTo(slate.venueName());
        assertThat(correct.path("venue").path("cityState").asString()).isEqualTo(slate.cityState());
        assertThat(correct.path("rivalryName").asString()).isEqualTo(slate.rivalryName());
        assertThat(correct.path("pickedTeamId").asString()).isEqualTo(slate.correctHomeId());
        assertThat(correct.path("rank").asInt()).isEqualTo(5);
        assertThat(correct.path("homeTeamScore").asInt()).isEqualTo(31);
        assertThat(correct.path("awayTeamScore").asInt()).isEqualTo(14);
        assertThat(correct.path("winningTeamId").asString()).isEqualTo(slate.correctHomeId());
        assertThat(correct.path("points").asInt()).isEqualTo(5);

        JsonNode wrong = matchup(current, slate.wrongId());
        assertThat(wrong.path("pickedTeamId").asString()).isEqualTo(slate.wrongHomeId());
        assertThat(wrong.path("rank").asInt()).isEqualTo(1);
        assertThat(wrong.path("homeTeamScore").asInt()).isEqualTo(10);
        assertThat(wrong.path("awayTeamScore").asInt()).isEqualTo(24);
        assertThat(wrong.path("winningTeamId").asString()).isEqualTo(slate.correctAwayId());
        assertThat(wrong.path("points").asInt()).isZero();
        assertThat(wrong.path("rivalryName").isNull()).isTrue();

        JsonNode alpha = matchup(current, slate.alphaId());
        assertThat(alpha.path("pickedTeamId").isNull()).isTrue();
        assertThat(alpha.path("rank").isNull()).isTrue();
        assertThat(alpha.path("homeTeamScore").isNull()).isTrue();
        assertThat(alpha.path("winningTeamId").isNull()).isTrue();
        assertThat(alpha.path("points").asInt()).isZero();

        JsonNode future = matchup(main.path("weeks").get(1).path("matchups"), slate.futureId());
        assertThat(future.path("pickedTeamId").asString()).isEqualTo(slate.futureAwayId());
        assertThat(future.path("rank").asInt()).isEqualTo(2);
        assertThat(future.path("homeTeamScore").isNull()).isTrue();
        assertThat(future.path("awayTeamScore").isNull()).isTrue();
        assertThat(future.path("winningTeamId").isNull()).isTrue();
        assertThat(future.path("points").isNull()).isTrue();
    }

    @Test
    void saveUpsertsOnlyTheCallerAndDropsEmptyPicks() throws Exception {
        Slate slate = arrange();
        String firstUid = "player-" + UUID.randomUUID();
        String secondUid = "player-" + UUID.randomUUID();
        Cookie first = player(firstUid);
        Cookie second = player(secondUid);

        JsonNode created = save(first, slate.currentWeekId(),
                pickRow(slate.correctId(), slate.correctHomeId(), 5) + "," + pickRow(slate.wrongId(), "  ", 9));
        assertThat(created).hasSize(1);
        assertThat(created.get(0).path("userId").asString()).isEqualTo(firstUid);
        assertThat(created.get(0).path("matchupId").asString()).isEqualTo(slate.correctId());
        assertThat(created.get(0).path("rank").asInt()).isEqualTo(5);
        assertThat(created.get(0).path("version").asInt()).isZero();
        assertThat(created.get(0).path("createUser").asString()).isEqualTo(firstUid);
        String pickId = created.get(0).path("id").asString();
        assertThat(pickId).isEqualTo(PickRepository.documentId(firstUid, slate.correctId()));
        assertThat(pickRepository.findByUserId(firstUid)).hasSize(1);

        JsonNode updated = save(first, slate.currentWeekId(),
                pickRow(slate.correctId(), slate.correctAwayId(), 4) + ","
                        + pickRow(slate.wrongId(), slate.wrongHomeId(), 1));
        assertThat(updated).hasSize(2);
        JsonNode corrected = findPick(updated, slate.correctId());
        assertThat(corrected.path("id").asString()).isEqualTo(pickId);
        assertThat(corrected.path("pickedTeamId").asString()).isEqualTo(slate.correctAwayId());
        assertThat(corrected.path("rank").asInt()).isEqualTo(4);
        assertThat(corrected.path("version").asInt()).isEqualTo(1);
        assertThat(corrected.path("createUser").asString()).isEqualTo(firstUid);
        assertThat(corrected.path("lastUpdateUser").asString()).isEqualTo(firstUid);
        assertThat(findPick(updated, slate.wrongId()).path("version").asInt()).isZero();

        JsonNode other = save(second, slate.currentWeekId(), pickRow(slate.correctId(), slate.correctHomeId(), 3));
        assertThat(other.get(0).path("userId").asString()).isEqualTo(secondUid);
        assertThat(other.get(0).path("id").asString()).isNotEqualTo(pickId);
        Pick stillFirst = pickRepository.findById(pickId).orElseThrow();
        assertThat(stillFirst.getUserId()).isEqualTo(firstUid);
        assertThat(stillFirst.getPickedTeamId()).isEqualTo(slate.correctAwayId());
        assertThat(stillFirst.getRank()).isEqualTo(4);

        JsonNode firstView = jsonMapper.readTree(
                mockMvc.perform(get("/api/game/main").cookie(first)).andExpect(status().isOk()).andReturn().getResponse()
                        .getContentAsString());
        JsonNode secondView = jsonMapper.readTree(
                mockMvc.perform(get("/api/game/main").cookie(second)).andExpect(status().isOk()).andReturn().getResponse()
                        .getContentAsString());
        assertThat(matchup(firstView.path("weeks").get(0).path("matchups"), slate.correctId()).path("pickedTeamId")
                .asString()).isEqualTo(slate.correctAwayId());
        assertThat(matchup(secondView.path("weeks").get(0).path("matchups"), slate.correctId()).path("pickedTeamId")
                .asString()).isEqualTo(slate.correctHomeId());

        mockMvc.perform(post("/api/game/picks").cookie(first).contentType(MediaType.APPLICATION_JSON)
                .content(picksBody(slate.currentWeekId(), pickRow(slate.wrongId(), slate.wrongHomeId(), 4))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(PickService.RANK_NOT_UNIQUE));

        JsonNode cleared = save(first, slate.currentWeekId(),
                pickRow(slate.correctId(), null, null) + "," + pickRow(slate.wrongId(), slate.correctAwayId(), 4));
        assertThat(cleared).hasSize(1);
        assertThat(cleared.get(0).path("matchupId").asString()).isEqualTo(slate.wrongId());
        assertThat(pickRepository.findById(pickId)).isEmpty();
        assertThat(pickRepository.findByUserId(secondUid)).hasSize(1);
    }

    @Test
    void saveRejectsAPickThatIsNotAValidWeeklyRank() throws Exception {
        Slate slate = arrange();
        Cookie player = player("player-" + UUID.randomUUID());

        reject(player, slate.currentWeekId(), pickRow(slate.correctId(), slate.correctHomeId(), 0),
                PickService.RANK_INVALID);
        reject(player, slate.currentWeekId(), pickRow(slate.correctId(), slate.correctHomeId(), 1_000_000),
                PickService.RANK_INVALID);
        reject(player, slate.currentWeekId(), pickRow(slate.correctId(), slate.correctHomeId(), null),
                PickService.RANK_INVALID);
        reject(player, slate.currentWeekId(), pickRow(slate.correctId(), slate.wrongHomeId(), 1),
                PickService.TEAM_NOT_IN_MATCHUP);
        reject(player, slate.currentWeekId(), pickRow(slate.futureId(), slate.futureAwayId(), 1),
                PickService.MATCHUP_WEEK);
        reject(player, slate.currentWeekId(), pickRow("missing-matchup", slate.correctHomeId(), 1),
                "Matchup not found");
        reject(player, "missing-week", pickRow(slate.correctId(), slate.correctHomeId(), 1),
                SeasonWeekService.NOT_FOUND);
        reject(player, slate.currentWeekId(),
                pickRow(slate.correctId(), slate.correctHomeId(), 1) + ","
                        + pickRow(slate.correctId(), slate.correctAwayId(), 2),
                PickService.MATCHUP_DUPLICATE);
        reject(player, slate.currentWeekId(),
                pickRow(slate.correctId(), slate.correctHomeId(), 1) + ","
                        + pickRow(slate.wrongId(), slate.wrongHomeId(), 1),
                PickService.RANK_NOT_UNIQUE);
        assertThat(pickRepository.findByUserId(userIds.get(userIds.size() - 1))).isEmpty();
    }

    private JsonNode save(Cookie player, String weekId, String picks) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/game/picks").cookie(player).contentType(MediaType.APPLICATION_JSON)
                        .content(picksBody(weekId, picks)))
                .andExpect(status().isOk()).andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private void reject(Cookie player, String weekId, String picks, String detail) throws Exception {
        mockMvc.perform(post("/api/game/picks").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content(picksBody(weekId, picks))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private Slate arrange() throws Exception {
        String token = token();
        String year = uniqueYear();
        int seasonYear = Integer.parseInt(year);
        LocalDate thursday = firstThursdayOnOrAfter(LocalDate.of(seasonYear, 9, 1));
        pin(thursday.plusDays(2));

        String venueName = "Bryant " + token;
        String cityState = "Tuscaloosa " + token;
        String venueId = createVenue(venueName, cityState);
        String correctHomeName = "Alabama " + token;
        String correctAwayName = "Auburn " + token;
        String correctHomeId = createTeam(correctHomeName, "Crimson Tide", venueId, true);
        String correctAwayId = createTeam(correctAwayName, "Tigers", venueId, true);
        String wrongHomeId = createTeam("Georgia " + token, "Bulldogs", venueId, true);
        String alphaIdTeam = createTeam("Alpha " + token, "A", venueId, true);
        String zuluIdTeam = createTeam("Zulu " + token, "Z", venueId, true);
        String futureHomeId = createTeam("Florida " + token, "Gators", venueId, true);
        String futureAwayId = createTeam("Kentucky " + token, "Wildcats", venueId, true);
        createTeam("Outsider " + token, "Independents", venueId, false);
        String rivalryName = "Iron Bowl " + token;
        rivalryIds.add(createRivalry(rivalryName, correctHomeId, correctAwayId));

        String seasonId = createSeason(year, thursday, LocalDate.of(seasonYear, 12, 20));
        String currentWeekId = createWeek(seasonId, 1, thursday, thursday.plusDays(6));
        LocalDate futureBegin = thursday.plusDays(7);
        String futureWeekId = createWeek(seasonId, 2, futureBegin, futureBegin.plusDays(6));
        String correctId = createMatchup(seasonId, currentWeekId, 1, thursday.plusDays(1).toString(), correctHomeId,
                correctAwayId, venueId, 31, 14);
        String wrongId = createMatchup(seasonId, currentWeekId, 1, thursday.toString(), wrongHomeId, correctAwayId,
                venueId, 10, 24);
        String alphaMatchupId = createMatchup(seasonId, currentWeekId, 1, thursday.toString(), alphaIdTeam,
                futureHomeId, venueId, null, null);
        String zuluMatchupId = createMatchup(seasonId, currentWeekId, 1, thursday.plusDays(2).toString(), zuluIdTeam,
                futureAwayId, venueId, null, null);
        String futureId = createMatchup(seasonId, futureWeekId, 2, futureBegin.toString(), futureHomeId, futureAwayId,
                venueId, 21, 7);

        return new Slate(seasonId, year, currentWeekId, futureWeekId, correctId, wrongId, alphaMatchupId, zuluMatchupId,
                futureId, correctHomeId, correctHomeName, correctAwayId, correctAwayName, wrongHomeId, futureAwayId,
                venueName, cityState, rivalryName);
    }

    private Cookie player(String uid) {
        userIds.add(uid);
        return new Cookie(SessionCookies.NAME, firebase.issueCookie(identity(uid, List.of("player"))));
    }

    private void pin(LocalDate date) {
        PinnedClockConfig.NOW.set(date.atTime(12, 0).atZone(PickScoring.LEAGUE_ZONE).toInstant());
    }

    private String createSeason(String year, LocalDate begin, LocalDate end) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"season":"%s","beginDate":"%s","endDate":"%s","isCurrent":true}
                        """.formatted(year, begin, end))).andExpect(status().isCreated()).andReturn();
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

    private String createRivalry(String name, String team1Id, String team2Id) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"rivalryName":"%s","team1Id":"%s","team2Id":"%s"}
                        """.formatted(escape(name), escape(team1Id), escape(team2Id)))).andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String createMatchup(String seasonId, String seasonWeekId, int weekNumber, String date, String homeTeamId,
            String awayTeamId, String venueId, Integer homeScore, Integer awayScore) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"seasonId":"%s","seasonWeekId":"%s","weekNumber":%d,"matchupDate":"%s","homeTeamId":"%s","awayTeamId":"%s","venueId":"%s","homeTeamScore":%s,"awayTeamScore":%s}
                        """.formatted(escape(seasonId), escape(seasonWeekId), weekNumber, escape(date), escape(homeTeamId),
                        escape(awayTeamId), escape(venueId), jsonNumber(homeScore), jsonNumber(awayScore))))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        matchupIds.add(id);
        return id;
    }

    private String idOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
    }

    private static JsonNode matchup(JsonNode matchups, String matchupId) {
        JsonNode found = null;

        for (JsonNode node : matchups) {
            if (matchupId.equals(node.path("matchupId").asString())) {
                found = node;
            }
        }

        assertThat(found).isNotNull();
        return found;
    }

    private static JsonNode findPick(JsonNode picks, String matchupId) {
        for (JsonNode pick : picks) {
            if (matchupId.equals(pick.path("matchupId").asString())) {
                return pick;
            }
        }

        throw new AssertionError("missing pick " + matchupId);
    }

    private static int indexOf(JsonNode matchups, String matchupId) {
        for (int i = 0; i < matchups.size(); i++) {
            if (matchupId.equals(matchups.get(i).path("matchupId").asString())) {
                return i;
            }
        }

        return -1;
    }

    private static String picksBody(String weekId, String picks) {
        return """
                {"seasonWeekId":"%s","userId":"not-the-caller","picks":[%s]}
                """.formatted(escape(weekId), picks);
    }

    private static String pickRow(String matchupId, String teamId, Integer rank) {
        return """
                {"matchupId":"%s","pickedTeamId":%s,"rank":%s}
                """.formatted(escape(matchupId), jsonString(teamId), jsonNumber(rank));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
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

    private record Slate(String seasonId, String year, String currentWeekId, String futureWeekId, String correctId,
            String wrongId, String alphaId, String zuluId, String futureId, String correctHomeId, String correctHomeName,
            String correctAwayId, String correctAwayName, String wrongHomeId, String futureAwayId, String venueName,
            String cityState, String rivalryName) {
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