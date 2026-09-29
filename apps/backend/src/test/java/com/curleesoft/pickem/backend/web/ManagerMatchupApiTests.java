package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.MatchupService;
import com.curleesoft.pickem.backend.service.SeasonWeekService;
import com.curleesoft.pickem.backend.service.TeamService;
import com.curleesoft.pickem.backend.service.VenueService;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ManagerMatchupApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private JsonMapper jsonMapper;

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

    private final List<String> matchupIds = new ArrayList<>();

    private final List<String> rivalryIds = new ArrayList<>();

    private final List<String> teamIds = new ArrayList<>();

    private final List<String> venueIds = new ArrayList<>();

    private final List<String> weekIds = new ArrayList<>();

    private final List<String> seasonIds = new ArrayList<>();

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
        matchupIds.clear();
        rivalryIds.clear();
        teamIds.clear();
        venueIds.clear();
        weekIds.clear();
        seasonIds.clear();
    }

    @Test
    void matchupsRequireTheManagerRole() throws Exception {
        Cookie player = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("player-" + UUID.randomUUID(), List.of("player"))));

        mockMvc.perform(get("/api/manager/matchups")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/matchups").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/manager/matchups/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/manager/matchups/missing")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/manager/matchups").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/matchups/missing").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/matchups").cookie(player).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/manager/matchups/missing").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/manager/matchups/missing").cookie(player)).andExpect(status().isForbidden());
    }

    @Test
    void matchupsCopySnapshotsDeriveTheWinnerAndRejectAnInvalidSlate() throws Exception {
        String token = token();
        Slate slate = slate(uniqueYear());
        String homeVenueId = createVenue("Neyland " + token, "Knoxville " + token);
        String neutralId = createVenue("Mercedes-Benz " + token, "Atlanta " + token);
        String homeId = createTeam("Tennessee " + token, "Volunteers", homeVenueId);
        String awayId = createTeam("Alabama " + token, "Crimson Tide", homeVenueId);
        String thirdId = createTeam("Georgia " + token, "Bulldogs", neutralId);
        String rivalryId = createRivalry("Third Saturday " + token, awayId, homeId);
        long externalId = externalId();

        mockMvc.perform(get("/api/manager/teams/" + homeId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.homeVenue.id").value(homeVenueId))
                .andExpect(jsonPath("$.homeVenue.name").value("Neyland " + token))
                .andExpect(jsonPath("$.homeVenue.cityState").value("Knoxville " + token));

        MvcResult created = mockMvc
                .perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(matchupBody(slate.seasonId(), slate.weekId(), null, slate.begin().plusDays(2).toString(),
                                homeId, awayId, neutralId, null, null, externalId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/manager/matchups/")))
                .andExpect(jsonPath("$.seasonId").value(slate.seasonId()))
                .andExpect(jsonPath("$.seasonWeekId").value(slate.weekId()))
                .andExpect(jsonPath("$.weekNumber").value(1))
                .andExpect(jsonPath("$.matchupDate").value(slate.begin().plusDays(2).toString()))
                .andExpect(jsonPath("$.homeTeamId").value(homeId)).andExpect(jsonPath("$.awayTeamId").value(awayId))
                .andExpect(jsonPath("$.homeTeam.id").value(homeId))
                .andExpect(jsonPath("$.homeTeam.name").value("Tennessee " + token))
                .andExpect(jsonPath("$.homeTeam.squad").value("Volunteers"))
                .andExpect(jsonPath("$.awayTeam.name").value("Alabama " + token))
                .andExpect(jsonPath("$.awayTeam.squad").value("Crimson Tide"))
                .andExpect(jsonPath("$.venueId").value(neutralId)).andExpect(jsonPath("$.venue.id").value(neutralId))
                .andExpect(jsonPath("$.venue.name").value("Mercedes-Benz " + token))
                .andExpect(jsonPath("$.venue.cityState").value("Atlanta " + token))
                .andExpect(jsonPath("$.rivalryName").value("Third Saturday " + token))
                .andExpect(jsonPath("$.cfbdGameId").value(externalId)).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.lastUpdateUser").value(managerUid)).andReturn();

        String matchupId = idOf(created);
        matchupIds.add(matchupId);
        JsonNode unplayed = jsonMapper.readTree(created.getResponse().getContentAsString());
        assertThat(unplayed.path("winningTeamId").isNull()).isTrue();
        assertThat(unplayed.path("homeTeamScore").isNull()).isTrue();
        assertThat(unplayed.path("awayTeamScore").isNull()).isTrue();
        assertThat(rivalryId).isNotBlank();

        mockMvc.perform(get("/api/manager/matchups/" + matchupId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.name").value("Mercedes-Benz " + token));

        MvcResult scored = mockMvc
                .perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId,
                                thirdId, homeVenueId, 17, 14, null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.winningTeamId").value(homeId))
                .andExpect(jsonPath("$.rivalryName").value(nullValue())).andReturn();
        matchupIds.add(idOf(scored));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, awayId,
                        neutralId, null, null, externalId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(MatchupService.CFBD_NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, homeId,
                        neutralId, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.TEAMS_EQUAL));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().minusDays(1).toString(), homeId,
                        awayId, neutralId, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.DATE_INVALID));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.end().plusDays(1).toString(), homeId,
                        awayId, neutralId, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.DATE_INVALID));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, "09/14/2026", homeId, awayId, neutralId, null,
                        null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.DATE_INVALID));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 99, slate.begin().toString(), homeId, awayId,
                        neutralId, null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MatchupService.WEEK_NUMBER_MISMATCH));

        Slate other = slate(uniqueYear());
        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(other.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, awayId,
                        neutralId, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.SEASON_MISMATCH));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), "missing-" + token, 1, slate.begin().toString(), homeId, awayId,
                        neutralId, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(SeasonWeekService.NOT_FOUND));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId,
                        "missing-" + token, neutralId, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(TeamService.NOT_FOUND));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, awayId,
                        "missing-" + token, null, null, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(VenueService.NOT_FOUND));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, awayId,
                        neutralId, 14, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MatchupService.SCORES_INCOMPLETE));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(matchupBody(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, awayId,
                        neutralId, 14, 14, null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.SCORES_TIED));

        mockMvc.perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                {"seasonWeekId":"%s","matchupDate":"%s","awayTeamId":"%s","venueId":"%s"}
                """.formatted(slate.weekId(), slate.begin(), awayId, neutralId))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("homeTeamId")));

        mockMvc.perform(put("/api/manager/matchups/" + matchupId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedMatchup(slate.seasonId(), slate.weekId(), 1, slate.begin().toString(), homeId, awayId,
                        homeVenueId, 10, 24, externalId, 0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.matchupDate").value(slate.begin().toString()))
                .andExpect(jsonPath("$.winningTeamId").value(awayId)).andExpect(jsonPath("$.homeTeamScore").value(10))
                .andExpect(jsonPath("$.awayTeamScore").value(24)).andExpect(jsonPath("$.venueId").value(homeVenueId))
                .andExpect(jsonPath("$.venue.name").value("Neyland " + token))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.rivalryName").value("Third Saturday " + token));

        mockMvc.perform(put("/api/manager/matchups/" + matchupId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedMatchup(slate.seasonId(), slate.weekId(), 1, slate.end().toString(), homeId, thirdId,
                        neutralId, 31, 7, externalId, 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.winningTeamId").value(homeId))
                .andExpect(jsonPath("$.matchupDate").value(slate.end().toString()))
                .andExpect(jsonPath("$.awayTeam.name").value("Georgia " + token))
                .andExpect(jsonPath("$.rivalryName").value(nullValue()));

        mockMvc.perform(put("/api/manager/matchups/" + matchupId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedMatchup(slate.seasonId(), slate.weekId(), 1, slate.end().toString(), homeId, thirdId,
                        neutralId, null, null, null, 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.cfbdGameId").value(nullValue()))
                .andExpect(jsonPath("$.winningTeamId").value(nullValue()));

        String cleared = mockMvc.perform(get("/api/manager/matchups/" + matchupId).cookie(manager))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode openGame = jsonMapper.readTree(cleared);
        assertThat(openGame.path("winningTeamId").isNull()).isTrue();
        assertThat(openGame.path("homeTeamScore").isNull()).isTrue();
        assertThat(openGame.path("cfbdGameId").isNull()).isTrue();

        String reused = createMatchup(slate.seasonId(), slate.weekId(), 1, slate.begin().plusDays(1).toString(), awayId,
                thirdId, neutralId, null, null, externalId);
        assertThat(reused).isNotBlank();

        mockMvc.perform(put("/api/manager/matchups/" + matchupId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedMatchup(slate.seasonId(), slate.weekId(), 1, slate.end().toString(), homeId, thirdId,
                        neutralId, 21, 21, null, 3)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(MatchupService.SCORES_TIED));

        mockMvc.perform(put("/api/manager/matchups/" + matchupId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedMatchup(slate.seasonId(), slate.weekId(), 1, slate.end().toString(), homeId, thirdId,
                        neutralId, null, null, null, 0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        mockMvc.perform(get("/api/manager/matchups/missing-" + token).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(MatchupService.NOT_FOUND));

        mockMvc.perform(delete("/api/manager/matchups/" + matchupId).cookie(manager)).andExpect(status().isNoContent());
        matchupIds.remove(matchupId);
        mockMvc.perform(get("/api/manager/matchups/" + matchupId).cookie(manager)).andExpect(status().isNotFound());

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/manager/matchups");
    }

    @Test
    void searchFiltersBySeasonWeekDateTeamAndVenue() throws Exception {
        String token = token();
        int baseYear = 6100 + ThreadLocalRandom.current().nextInt(400);
        Slate older = slate(String.valueOf(baseYear));
        Slate newer = slate(String.valueOf(baseYear + 1));
        String weekTwo = createWeek(older.seasonId(), 2, older.begin().plusDays(7), older.begin().plusDays(13));
        String mainId = createVenue("Main " + token, "City " + token);
        String neutralId = createVenue("Neutral " + token, "Other " + token);
        String alphaId = createTeam("Alpha " + token, "Squad A", mainId);
        String zuluId = createTeam("Zulu " + token, "Squad Z", mainId);
        String awayId = createTeam("Away " + token, "Squad W", mainId);
        String otherAwayId = createTeam("Other Away " + token, "Squad O", neutralId);
        long externalId = externalId();

        String alphaThursday = createMatchup(older.seasonId(), older.weekId(), 1, older.begin().toString(), alphaId,
                awayId, mainId, null, null, externalId);
        String zuluThursday = createMatchup(older.seasonId(), older.weekId(), 1, older.begin().toString(), zuluId,
                otherAwayId, neutralId, null, null, null);
        String alphaSaturday = createMatchup(older.seasonId(), older.weekId(), 1, older.begin().plusDays(2).toString(),
                alphaId, otherAwayId, mainId, 7, 3, null);
        String weekTwoGame = createMatchup(older.seasonId(), weekTwo, 2, older.begin().plusDays(7).toString(), zuluId,
                awayId, mainId, null, null, null);
        String nextSeason = createMatchup(newer.seasonId(), newer.weekId(), 1, newer.begin().toString(), alphaId, awayId,
                mainId, null, null, null);

        mockMvc.perform(get("/api/manager/matchups/" + alphaSaturday).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.winningTeamId").value(alphaId));

        String olderListed = mockMvc
                .perform(get("/api/manager/matchups").cookie(manager).param("seasonId", older.seasonId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4)).andReturn().getResponse()
                .getContentAsString();
        JsonNode olderGames = jsonMapper.readTree(olderListed);
        assertThat(indexOf(olderGames, alphaThursday)).isLessThan(indexOf(olderGames, zuluThursday));
        assertThat(indexOf(olderGames, zuluThursday)).isLessThan(indexOf(olderGames, alphaSaturday));
        assertThat(indexOf(olderGames, alphaSaturday)).isLessThan(indexOf(olderGames, weekTwoGame));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonId", newer.seasonId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(nextSeason));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonWeekId", older.weekId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonId", older.seasonId()).param("weekNumber",
                "2")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(weekTwoGame));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonId", older.seasonId())
                .param("matchupDate", older.begin().plusDays(2).toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(alphaSaturday));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("teamId", otherAwayId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(zuluThursday))
                .andExpect(jsonPath("$[1].id").value(alphaSaturday));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("teamId", awayId).param("venueId", neutralId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("venueId", neutralId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(zuluThursday));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("cfbdGameId", Long.toString(externalId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(alphaThursday));

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("weekNumber", "nope"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request is invalid"));

        String all = mockMvc.perform(get("/api/manager/matchups").cookie(manager)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        JsonNode games = jsonMapper.readTree(all);
        assertThat(indexOf(games, weekTwoGame)).isLessThan(indexOf(games, nextSeason));
    }

    private Slate slate(String year) throws Exception {
        int seasonYear = Integer.parseInt(year);
        LocalDate begin = firstThursdayOnOrAfter(LocalDate.of(seasonYear, 9, 1));
        String seasonId = createSeason(year, begin, LocalDate.of(seasonYear, 12, 20));
        String weekId = createWeek(seasonId, 1, begin, begin.plusDays(6));
        return new Slate(seasonId, weekId, begin, begin.plusDays(6));
    }

    private String createSeason(String year, LocalDate begin, LocalDate end) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"season":"%s","beginDate":"%s","endDate":"%s","isCurrent":false}
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

    private String createTeam(String name, String squad, String venueId) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"teamName":"%s","squadName":"%s","conferenceMember":true,"homeVenueId":"%s"}
                        """.formatted(escape(name), escape(squad), escape(venueId)))).andExpect(status().isCreated())
                .andReturn();
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
        String id = idOf(result);
        rivalryIds.add(id);
        return id;
    }

    private String createMatchup(String seasonId, String seasonWeekId, int weekNumber, String date, String homeTeamId,
            String awayTeamId, String venueId, Integer homeScore, Integer awayScore, Long cfbdGameId) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(matchupBody(seasonId, seasonWeekId, weekNumber, date, homeTeamId, awayTeamId, venueId,
                                homeScore, awayScore, cfbdGameId)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        matchupIds.add(id);
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

    private static String matchupBody(String seasonId, String seasonWeekId, Integer weekNumber, String date,
            String homeTeamId, String awayTeamId, String venueId, Integer homeScore, Integer awayScore,
            Long cfbdGameId) {
        return versionedMatchup(seasonId, seasonWeekId, weekNumber, date, homeTeamId, awayTeamId, venueId, homeScore,
                awayScore, cfbdGameId, 9);
    }

    private static String versionedMatchup(String seasonId, String seasonWeekId, Integer weekNumber, String date,
            String homeTeamId, String awayTeamId, String venueId, Integer homeScore, Integer awayScore, Long cfbdGameId,
            long version) {
        return """
                {"seasonId":"%s","seasonWeekId":"%s","weekNumber":%s,"matchupDate":"%s","homeTeamId":"%s","awayTeamId":"%s","venueId":"%s","homeTeamScore":%s,"awayTeamScore":%s,"cfbdGameId":%s,"winningTeamId":"client-winner","rivalryName":"Client Rivalry","homeTeam":{"id":"wrong","name":"Wrong","squad":"Wrong"},"awayTeam":{"id":"wrong","name":"Wrong","squad":"Wrong"},"venue":{"id":"wrong","name":"Wrong","cityState":"Nowhere"},"createUser":"hacker","version":%d}
                """.formatted(escape(seasonId), escape(seasonWeekId), jsonNumber(weekNumber), escape(date),
                escape(homeTeamId), escape(awayTeamId), escape(venueId), jsonNumber(homeScore), jsonNumber(awayScore),
                jsonNumber(cfbdGameId), version);
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

    private static long externalId() {
        return ThreadLocalRandom.current().nextLong(3_000_000_000L, 9_000_000_000_000L);
    }

    private static String uniqueYear() {
        return String.valueOf(6100 + ThreadLocalRandom.current().nextInt(400));
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

    private record Slate(String seasonId, String weekId, LocalDate begin, LocalDate end) {
    }
}
