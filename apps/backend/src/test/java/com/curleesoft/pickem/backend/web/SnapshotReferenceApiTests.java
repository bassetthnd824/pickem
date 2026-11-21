package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.Objects;
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
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.PickScoring;
import com.curleesoft.pickem.backend.service.ReferenceGuard;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SnapshotReferenceApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private SeasonWeekRepository seasonWeekRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RivalryRepository rivalryRepository;

    @Autowired
    private MatchupRepository matchupRepository;

    @Autowired
    private PickRepository pickRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<String> seasonIds = new ArrayList<>();

    private final List<String> weekIds = new ArrayList<>();

    private final List<String> venueIds = new ArrayList<>();

    private final List<String> teamIds = new ArrayList<>();

    private final List<String> rivalryIds = new ArrayList<>();

    private final List<String> matchupIds = new ArrayList<>();

    private final List<String> pickIds = new ArrayList<>();

    private final List<String> userIds = new ArrayList<>();

    private Cookie manager;

    private Cookie player;

    @BeforeEach
    void signIn() {
        firebase.clear();
        manager = new Cookie(
                SessionCookies.NAME,
                firebase.issueCookie(identity("manager-" + UUID.randomUUID(), List.of("manager")))
        );
        player = new Cookie(
                SessionCookies.NAME,
                firebase.issueCookie(identity("player-" + UUID.randomUUID(), List.of("player")))
        );
    }

    @AfterEach
    void deleteFixtures() {
        PinnedClockConfig.NOW.set(Instant.now());
        pickIds.forEach(pickRepository::delete);
        userIds.forEach(userRepository::delete);
        matchupIds.forEach(matchupRepository::delete);
        rivalryIds.forEach(rivalryRepository::delete);
        teamIds.forEach(teamRepository::delete);
        venueIds.forEach(venueRepository::delete);
        weekIds.forEach(seasonWeekRepository::delete);
        seasonIds.forEach(seasonRepository::delete);
        pickIds.clear();
        userIds.clear();
        matchupIds.clear();
        rivalryIds.clear();
        teamIds.clear();
        venueIds.clear();
        weekIds.clear();
        seasonIds.clear();
    }

    @Test
    void renamesRefreshSnapshotsAndDeletesNameTheBlockingRelation() throws Exception {
        String token = token();
        String year = uniqueYear();
        int seasonYear = Integer.parseInt(year);
        LocalDate begin = firstThursdayOnOrAfter(LocalDate.of(seasonYear, 9, 1));
        LocalDate end = LocalDate.of(seasonYear, 12, 20);
        pin(begin.plusDays(2));

        String seasonId = createSeason(year, begin, end);
        String weekId = createWeek(seasonId, 1, begin, begin.plusDays(6));
        String homeVenueId = createVenue("Neyland " + token, "Knoxville " + token);
        String neutralId = createVenue("Mercedes " + token, "Atlanta " + token);
        String otherVenueId = createVenue("Sanford " + token, "Athens " + token);
        String homeId = createTeam("Home " + token, "Hosts", homeVenueId);
        String awayId = createTeam("Away " + token, "Guests", otherVenueId);
        String rivalryOnlyId = createTeam("Rival " + token, "Only", otherVenueId);
        String rivalryId = createRivalry("Iron " + token, homeId, rivalryOnlyId);
        String matchupDate = begin.plusDays(2).toString();
        String matchupId = createMatchup(seasonId, weekId, matchupDate, homeId, awayId, neutralId);

        String renamedHome = "Home Red " + token;
        String renamedSquad = "Crimson";
        mockMvc.perform(
                put("/api/manager/teams/" + homeId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody(renamedHome, renamedSquad, homeVenueId, 0))
        )
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonId", seasonId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(matchupId))
                .andExpect(jsonPath("$[0].homeTeam.name").value(renamedHome))
                .andExpect(jsonPath("$[0].homeTeam.squad").value(renamedSquad))
                .andExpect(jsonPath("$[0].homeTeamScore").value(21)).andExpect(jsonPath("$[0].awayTeamScore").value(14))
                .andExpect(jsonPath("$[0].winningTeamId").value(homeId));

        JsonNode main = gameMain();
        assertThat(main.path("seasonId").asString()).isEqualTo(seasonId);
        JsonNode game = game(main, matchupId);
        assertThat(game.path("homeTeam").path("name").asString()).isEqualTo(renamedHome);
        assertThat(game.path("homeTeam").path("squad").asString()).isEqualTo(renamedSquad);
        assertThat(game.path("awayTeam").path("name").asString()).isEqualTo("Away " + token);

        mockMvc.perform(get("/api/manager/rivalries/" + rivalryId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.team1.name").value(renamedHome))
                .andExpect(jsonPath("$.team2.name").value("Rival " + token));

        String renamedAway = "Away Blue " + token;
        mockMvc.perform(
                put("/api/manager/teams/" + awayId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody(renamedAway, "Guests", otherVenueId, 0))
        )
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonId", seasonId))
                .andExpect(jsonPath("$[0].awayTeam.name").value(renamedAway))
                .andExpect(jsonPath("$[0].awayTeam.squad").value("Guests"));
        assertThat(game(gameMain(), matchupId).path("awayTeam").path("name").asString()).isEqualTo(renamedAway);

        String renamedVenue = "Mercedes-Benz " + token;
        String renamedCity = "Atlanta, GA " + token;
        mockMvc.perform(
                put("/api/manager/venues/" + neutralId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(venueBody(renamedVenue, renamedCity, 0))
        )
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/matchups").cookie(manager).param("seasonId", seasonId))
                .andExpect(jsonPath("$[0].venue.name").value(renamedVenue))
                .andExpect(jsonPath("$[0].venue.cityState").value(renamedCity))
                .andExpect(jsonPath("$[0].homeTeam.name").value(renamedHome));
        JsonNode refreshedVenue = game(gameMain(), matchupId).path("venue");
        assertThat(refreshedVenue.path("name").asString()).isEqualTo(renamedVenue);
        assertThat(refreshedVenue.path("cityState").asString()).isEqualTo(renamedCity);

        String renamedHomeCity = "Knoxville, TN " + token;
        mockMvc.perform(
                put("/api/manager/venues/" + homeVenueId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(venueBody("Neyland " + token, renamedHomeCity, 0))
        )
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/teams/" + homeId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.homeVenue.cityState").value(renamedHomeCity))
                .andExpect(jsonPath("$.teamName").value(renamedHome));
        mockMvc.perform(get("/api/manager/matchups/" + matchupId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.cityState").value(renamedCity));

        mockMvc.perform(delete("/api/manager/seasons/" + seasonId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.SEASON_WEEKS));
        mockMvc.perform(delete("/api/manager/season-weeks/" + weekId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.MATCHUPS));
        mockMvc.perform(delete("/api/manager/teams/" + homeId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.MATCHUPS));
        mockMvc.perform(delete("/api/manager/teams/" + rivalryOnlyId).cookie(manager))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(ReferenceGuard.RIVALRIES));
        mockMvc.perform(delete("/api/manager/venues/" + neutralId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.MATCHUPS));
        mockMvc.perform(delete("/api/manager/venues/" + homeVenueId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.TEAMS));

        String userId = "picker-" + UUID.randomUUID();
        userIds.add(userId);
        User user = new User();
        user.setId(userId);
        user.setUid(userId);
        user.setEmailAddr(userId + "@example.com");
        user.setFirstName("Pick");
        user.setLastName("Player");
        user.setNickName("Picker");
        user.setThemeId("light");
        user.setRoles(List.of("player"));
        userRepository.save(user);
        String pickId = PickRepository.documentId(userId, matchupId);
        pickIds.add(pickId);
        Pick pick = new Pick();
        pick.setId(pickId);
        pick.setUserId(userId);
        pick.setMatchupId(matchupId);
        pick.setSeasonId(seasonId);
        pick.setSeasonWeekId(weekId);
        pick.setPickedTeamId(homeId);
        pick.setRank(1);
        pickRepository.save(pick);

        mockMvc.perform(delete("/api/manager/matchups/" + matchupId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.PICKS));
        assertThat(pickRepository.findById(pickId)).isPresent();
        mockMvc.perform(delete("/api/manager/users/" + userId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.PICKS));
        assertThat(userRepository.findById(userId)).isPresent();
        assertThat(firebase.refreshRevoked(userId)).isFalse();
        assertThat(firebase.claimWriteCount(userId)).isZero();

        seasonWeekRepository.delete(weekId);
        weekIds.remove(weekId);
        mockMvc.perform(delete("/api/manager/seasons/" + seasonId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.MATCHUPS));

        matchupRepository.delete(matchupId);
        matchupIds.remove(matchupId);
        mockMvc.perform(delete("/api/manager/seasons/" + seasonId).cookie(manager)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ReferenceGuard.PICKS));

        pickRepository.delete(pickId);
        pickIds.remove(pickId);
        mockMvc.perform(delete("/api/manager/seasons/" + seasonId).cookie(manager)).andExpect(status().isNoContent());
        seasonIds.remove(seasonId);
        mockMvc.perform(delete("/api/manager/users/" + userId).cookie(manager)).andExpect(status().isNoContent());
        userIds.remove(userId);
        mockMvc.perform(get("/api/manager/seasons/" + seasonId).cookie(manager)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/manager/users/" + userId).cookie(manager)).andExpect(status().isNotFound());
    }

    private JsonNode gameMain() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/game/main").cookie(player)).andExpect(status().isOk()).andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private static JsonNode game(JsonNode main, String matchupId) {

        for (JsonNode week : main.path("weeks")) {

            for (JsonNode matchup : week.path("matchups")) {

                if (matchupId.equals(matchup.path("matchupId").asString())) {
                    return matchup;

                }

            }

        }

        throw new AssertionError("matchup " + matchupId + " missing from game main");
    }

    @Test
    void retryOfACommittedRenameRefreshesSnapshotsWithoutWritingAgain() throws Exception {
        String token = token();
        String venueId = createVenue("Resume Venue " + token, "City " + token);
        String homeId = createTeam("Resume " + token, "Hosts", venueId);
        String awayId = createTeam("Other " + token, "Guests", venueId);
        String rivalryId = createRivalry("Resume Rivalry " + token, homeId, awayId);
        String renamed = "Resume Red " + token;
        Team stored = teamRepository.findById(homeId).orElseThrow();
        stored.setTeamName(renamed);
        teamRepository.save(stored);
        teamRepository.patch(homeId, team -> {
            VenueSnapshot venue = Objects.requireNonNull(team.getHomeVenue(), "homeVenue");
            team.setHomeVenue(new VenueSnapshot(venue.getId(), venue.getName(), "Moved " + token));
            return true;

        });

        mockMvc.perform(get("/api/manager/rivalries/" + rivalryId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.team1.name").value("Resume " + token));

        mockMvc.perform(
                put("/api/manager/teams/" + homeId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody(renamed, "Hosts", venueId, 0))
        )
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.teamName").value(renamed));

        mockMvc.perform(get("/api/manager/rivalries/" + rivalryId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.team1.name").value(renamed)).andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(
                put("/api/manager/teams/" + homeId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody(renamed, "Hosts", venueId, 4))
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Stale version")));
    }

    private String createSeason(String year, LocalDate begin, LocalDate end) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                                {"season":"%s","beginDate":"%s","endDate":"%s","isCurrent":true}
                                """.formatted(year, begin, end))
                )
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        seasonIds.add(id);
        return id;
    }

    private String createWeek(String seasonId, int weekNumber, LocalDate begin, LocalDate end) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"seasonId":"%s","weekNumber":%d,"beginDate":"%s","endDate":"%s"}
                                        """.formatted(seasonId, weekNumber, begin, end))
                )
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        weekIds.add(id);
        return id;
    }

    private String createVenue(String name, String cityState) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"venueName":"%s","cityState":"%s"}
                        """.formatted(name, cityState))).andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        venueIds.add(id);
        return id;
    }

    private String createTeam(String name, String squad, String venueId) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                                .content(teamBody(name, squad, venueId, 9))
                )
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        teamIds.add(id);
        return id;
    }

    private String createRivalry(String name, String team1Id, String team2Id) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rivalryName":"%s","team1Id":"%s","team2Id":"%s"}
                                        """.formatted(name, team1Id, team2Id))
                )
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        rivalryIds.add(id);
        return id;
    }

    private String createMatchup(
            String seasonId,
            String weekId,
            String date,
            String homeId,
            String awayId,
            String venueId
    ) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        post("/api/manager/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {"seasonId":"%s","seasonWeekId":"%s","matchupDate":"%s","homeTeamId":"%s","awayTeamId":"%s","venueId":"%s","homeTeamScore":21,"awayTeamScore":14}
                                                """
                                                .formatted(seasonId, weekId, date, homeId, awayId, venueId)
                                )
                )
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        matchupIds.add(id);
        return id;
    }

    private String idOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
    }

    private static String teamBody(String name, String squad, String venueId, long version) {
        return """
                {"teamName":"%s","squadName":"%s","conferenceMember":true,"homeVenueId":"%s","version":%d}
                """.formatted(name, squad, venueId, version);
    }

    private static String venueBody(String name, String cityState, long version) {
        return """
                {"venueName":"%s","cityState":"%s","version":%d}
                """.formatted(name, cityState, version);
    }

    private static void pin(LocalDate date) {
        PinnedClockConfig.NOW.set(date.atTime(12, 0).atZone(PickScoring.LEAGUE_ZONE).toInstant());
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueYear() {
        return String.valueOf(8000 + ThreadLocalRandom.current().nextInt(500));
    }

    private static LocalDate firstThursdayOnOrAfter(LocalDate date) {
        LocalDate cursor = date;

        while (cursor.getDayOfWeek() != DayOfWeek.THURSDAY) {
            cursor = cursor.plusDays(1);

        }

        return cursor;
    }

    private static VerifiedIdentity identity(String uid, List<String> roles) {
        return new VerifiedIdentity(
                uid,
                uid + "@example.com",
                "Test User",
                "Test",
                "User",
                "google.com",
                RoleClaims.forRoles(roles)
        );
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
