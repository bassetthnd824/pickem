package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.cfbd.CfbdClientException;
import com.curleesoft.pickem.backend.cfbd.CfbdGame;
import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.CfbdPause;
import com.curleesoft.pickem.backend.service.CfbdScoreService;
import com.curleesoft.pickem.backend.service.CfbdScoreService.ScoreSyncResult;
import com.curleesoft.pickem.backend.service.PickScoring;
import com.curleesoft.pickem.backend.service.SeasonService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CfbdScoreJobTests extends FirestoreEmulatorSupport {

    private static final int RANK = 8;

    private static int nextYear = 7300;

    private static long nextGameId = 8_300_000_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CfbdScoreService cfbdScoreService;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private SeasonWeekRepository seasonWeekRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private MatchupRepository matchupRepository;

    @Autowired
    private PickRepository pickRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @MockitoBean
    private CfbdClient cfbdClient;

    @MockitoBean
    private CfbdPause cfbdPause;

    private final List<String> seasonIds = new ArrayList<>();

    private final List<String> weekIds = new ArrayList<>();

    private final List<String> teamIds = new ArrayList<>();

    private final List<String> venueIds = new ArrayList<>();

    private final List<String> matchupIds = new ArrayList<>();

    private final List<String> pickIds = new ArrayList<>();

    private final List<String> userIds = new ArrayList<>();

    private LocalDate today;

    @BeforeEach
    void pinToday() {
        int year = nextYear++;
        today = LocalDate.of(year, 10, 4);
        pin(today);
    }

    @AfterEach
    void cleanup() {
        pickIds.forEach(pickRepository::delete);
        matchupIds.forEach(matchupRepository::delete);
        weekIds.forEach(seasonWeekRepository::delete);
        teamIds.forEach(teamRepository::delete);
        venueIds.forEach(venueRepository::delete);
        userIds.forEach(userRepository::delete);
        seasonIds.forEach(seasonRepository::delete);
        pickIds.clear();
        matchupIds.clear();
        weekIds.clear();
        teamIds.clear();
        venueIds.clear();
        userIds.clear();
        seasonIds.clear();
    }

    @Test
    void completedGameUpdatesMainAndLeaderboardWithoutAnotherPlayerSave() throws Exception {
        Slate slate = slate(true);
        long gameId = nextGameId();
        long futureGameId = nextGameId();
        Matchup todayGame = matchup(slate, today, gameId, null, null, null);
        Matchup futureGame = matchup(slate, today.plusDays(2), futureGameId, null, null, null);
        Matchup manual = matchup(slate, today, null, null, null, null);
        String uid = player();
        pick(uid, slate, todayGame, slate.home().getId(), RANK);
        Cookie cookie = cookie(uid);
        stubRegular(game(gameId, true, 24, 17), game(futureGameId, true, 30, 10));

        JsonNode before = matchupNode(main(cookie), todayGame.getId());
        assertThat(before.path("homeTeamScore").isNull() || before.path("homeTeamScore").isMissingNode()).isTrue();
        assertThat(before.path("points").asInt()).isZero();
        assertThat(before.path("pickedTeamId").asString()).isEqualTo(slate.home().getId());
        assertThat(standing(leaderboard(cookie), uid).path("score").asLong()).isZero();

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(loopback())).andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("cfbd-scores")).andExpect(jsonPath("$.updated").value(1));

        Matchup scored = reload(todayGame);
        assertThat(scored.getHomeTeamScore()).isEqualTo(24);
        assertThat(scored.getAwayTeamScore()).isEqualTo(17);
        assertThat(scored.getWinningTeamId()).isEqualTo(slate.home().getId());
        assertThat(scored.getLastUpdateUser()).isEqualTo("internal-job");
        assertThat(scored.getVersion()).isEqualTo(1L);
        assertThat(reload(futureGame).getHomeTeamScore()).isNull();
        assertThat(reload(manual).getHomeTeamScore()).isNull();
        assertThat(reload(manual).getWinningTeamId()).isNull();

        JsonNode after = matchupNode(main(cookie), todayGame.getId());
        assertThat(after.path("homeTeamScore").asInt()).isEqualTo(24);
        assertThat(after.path("awayTeamScore").asInt()).isEqualTo(17);
        assertThat(after.path("winningTeamId").asString()).isEqualTo(slate.home().getId());
        assertThat(after.path("points").asInt()).isEqualTo(RANK);
        assertThat(after.path("pickedTeamId").asString()).isEqualTo(slate.home().getId());
        assertThat(standing(leaderboard(cookie), uid).path("score").asLong()).isEqualTo(RANK);

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(loopback())).andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(0));
        assertThat(reload(todayGame).getVersion()).isEqualTo(1L);
    }

    @Test
    void awayWinUsesTheAwayTeam() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup game = matchup(slate, today, gameId, null, null, null);
        stubRegular(game(gameId, true, 10, 24));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result).isEqualTo(new ScoreSyncResult(1, 1));
        Matchup scored = reload(game);
        assertThat(scored.getHomeTeamScore()).isEqualTo(10);
        assertThat(scored.getAwayTeamScore()).isEqualTo(24);
        assertThat(scored.getWinningTeamId()).isEqualTo(slate.away().getId());
    }

    @Test
    void uncompletedOrMissingPointsLeaveStoredScores() {
        Slate slate = slate(true);
        long openId = nextGameId();
        long partialId = nextGameId();
        Matchup open = matchup(slate, today, openId, null, null, null);
        Matchup manager = matchup(slate, today, partialId, null, 17, 14);
        stubRegular(game(openId, false, 10, 3), game(partialId, true, null, 9));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result).isEqualTo(new ScoreSyncResult(2, 0));
        assertThat(reload(open).getHomeTeamScore()).isNull();
        assertThat(reload(open).getVersion()).isZero();
        Matchup kept = reload(manager);
        assertThat(kept.getHomeTeamScore()).isEqualTo(17);
        assertThat(kept.getAwayTeamScore()).isEqualTo(14);
        assertThat(kept.getWinningTeamId()).isEqualTo(slate.home().getId());
        assertThat(kept.getVersion()).isZero();
    }

    @Test
    void sameCompletedScoresAreNotWrittenAgain() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup game = matchup(slate, today, gameId, null, 24, 17);
        stubRegular(game(gameId, true, 24, 17));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result).isEqualTo(new ScoreSyncResult(1, 0));
        assertThat(reload(game).getVersion()).isZero();
        verify(cfbdClient).games(today.getYear(), null, "regular");
    }

    @Test
    void differentCompletedScoresReplaceManagerScores() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup game = matchup(slate, today, gameId, null, 21, 14);
        stubRegular(game(gameId, true, 14, 21));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result.updated()).isEqualTo(1);
        Matchup scored = reload(game);
        assertThat(scored.getHomeTeamScore()).isEqualTo(14);
        assertThat(scored.getAwayTeamScore()).isEqualTo(21);
        assertThat(scored.getWinningTeamId()).isEqualTo(slate.away().getId());
    }

    @Test
    void tiedCompletedGameIsNotStored() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup game = matchup(slate, today, gameId, null, null, null);
        stubRegular(game(gameId, true, 14, 14));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result).isEqualTo(new ScoreSyncResult(1, 0));
        Matchup kept = reload(game);
        assertThat(kept.getHomeTeamScore()).isNull();
        assertThat(kept.getAwayTeamScore()).isNull();
        assertThat(kept.getWinningTeamId()).isNull();
    }

    @Test
    void noDueMatchupsDoesNotCallCfbd() throws Exception {
        Slate slate = slate(true);
        Season other = season(today.plusYears(1), false);
        SeasonWeek otherWeek = week(other, 1, today.minusDays(3), today.plusDays(3));
        matchup(new Slate(other, otherWeek, slate.home(), slate.away(), slate.venue()), today, nextGameId(), null, null,
                null);
        assertThat(seasonService.current().getId()).isEqualTo(slate.season().getId());

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(loopback())).andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(0));

        verifyNoInteractions(cfbdClient);
    }

    @Test
    void noCurrentSeasonDoesNotCallCfbd() {
        List<Season> flagged = new ArrayList<>();
        for (Season season : seasonRepository.findAll()) {
            if (season.isCurrent()) {
                flagged.add(season);
                season.setCurrent(false);
                seasonRepository.save(season, "test");
            }
        }
        try {
            pin(LocalDate.of(1000, 6, 1));
            ScoreSyncResult result = cfbdScoreService.syncToday();
            assertThat(result).isEqualTo(new ScoreSyncResult(0, 0));
            verifyNoInteractions(cfbdClient);
        } finally {
            for (Season season : flagged) {
                season.setCurrent(true);
                seasonRepository.save(season, "test");
            }
            pin(today);
        }
    }

    @Test
    void yesterdayUnscoredGameIsScoredAfterMidnight() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup spilled = matchup(slate, today.minusDays(1), gameId, null, null, null);
        stubRegular(game(gameId, true, 28, 14));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result.updated()).isEqualTo(1);
        Matchup scored = reload(spilled);
        assertThat(scored.getHomeTeamScore()).isEqualTo(28);
        assertThat(scored.getWinningTeamId()).isEqualTo(slate.home().getId());
    }

    @Test
    void yesterdayScoredAndOlderUnscoredGamesAreSkipped() {
        Slate slate = slate(true);
        Matchup yesterday = matchup(slate, today.minusDays(1), nextGameId(), null, 7, 3);
        Matchup older = matchup(slate, today.minusDays(2), nextGameId(), null, null, null);

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result).isEqualTo(new ScoreSyncResult(0, 0));
        assertThat(reload(yesterday).getHomeTeamScore()).isEqualTo(7);
        assertThat(reload(older).getHomeTeamScore()).isNull();
        verifyNoInteractions(cfbdClient);
    }

    @Test
    void postseasonWeekUsesThePostseasonGamesCall() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup bowl = matchup(slate, today, gameId, Integer.valueOf(101), null,
                null);
        when(cfbdClient.games(eq(today.getYear()), isNull(), eq("postseason")))
                .thenReturn(List.of(game(gameId, true, 35, 17)));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result.updated()).isEqualTo(1);
        assertThat(reload(bowl).getHomeTeamScore()).isEqualTo(35);
        verify(cfbdClient, never()).games(anyInt(), isNull(), eq("regular"));
    }

    @Test
    void cfbdFailureIsLoggedAndLeavesTheMatchup(CapturedOutput output) throws Exception {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup game = matchup(slate, today, gameId, null, null, null);
        when(cfbdClient.games(eq(today.getYear()), isNull(), eq("regular")))
                .thenThrow(new CfbdClientException(503, CfbdClientException.UNAVAILABLE));

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(loopback()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(CfbdClientException.UNAVAILABLE));

        assertThat(output).contains("CFBD score sync failed");
        Matchup kept = reload(game);
        assertThat(kept.getHomeTeamScore()).isNull();
        assertThat(kept.getVersion()).isZero();
        verify(cfbdClient, times(3)).games(today.getYear(), null, "regular");
    }

    @Test
    void missingKeyFailsOnceAndDoesNotWrite(CapturedOutput output) throws Exception {
        Slate slate = slate(true);
        Matchup game = matchup(slate, today, nextGameId(), null, 9, 6);
        when(cfbdClient.games(eq(today.getYear()), isNull(), eq("regular")))
                .thenThrow(new CfbdClientException(CfbdClientException.MISSING_KEY));

        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(loopback()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(CfbdClientException.MISSING_KEY));

        assertThat(output).contains("CFBD score sync failed");
        assertThat(reload(game).getHomeTeamScore()).isEqualTo(9);
        assertThat(reload(game).getVersion()).isZero();
        verify(cfbdClient, times(1)).games(today.getYear(), null, "regular");
        verifyNoInteractions(cfbdPause);
    }

    @Test
    void transientFailureRetriesThenWrites() {
        Slate slate = slate(true);
        long gameId = nextGameId();
        Matchup game = matchup(slate, today, gameId, null, null, null);
        when(cfbdClient.games(eq(today.getYear()), isNull(), eq("regular")))
                .thenThrow(new CfbdClientException(429, CfbdClientException.RATE_LIMITED))
                .thenReturn(List.of(game(gameId, true, 31, 20)));

        ScoreSyncResult result = cfbdScoreService.syncToday();

        assertThat(result.updated()).isEqualTo(1);
        assertThat(reload(game).getAwayTeamScore()).isEqualTo(20);
        verify(cfbdPause).pause(1);
        verify(cfbdClient, times(2)).games(today.getYear(), null, "regular");
    }

    private void stubRegular(CfbdGame... rows) {
        when(cfbdClient.games(eq(today.getYear()), isNull(), eq("regular"))).thenReturn(List.of(rows));
    }

    private CfbdGame game(long id, boolean completed, Integer homePoints, Integer awayPoints) {
        return new CfbdGame(id, today.getYear(), 1, "regular", today + "T23:30:00Z", completed,
                "Stadium", Long.valueOf(1), 100L, "Home", homePoints, 200L, "Away", awayPoints, Boolean.FALSE, "SEC",
                "SEC");
    }

    private Slate slate(boolean current) {
        Venue venue = venue();
        Team home = team("Home", venue);
        Team away = team("Away", venue);
        Season season = season(today, current);
        SeasonWeek week = week(season, 1, today.minusDays(3), today.plusDays(3));
        return new Slate(season, week, home, away, venue);
    }

    private Season season(LocalDate day, boolean current) {
        Season season = new Season();
        season.setSeason(Integer.toString(day.getYear()));
        season.setBeginDate(day.withMonth(8).withDayOfMonth(1).toString());
        season.setEndDate(LocalDate.of(day.getYear(), 12, 20).toString());
        season.setCurrent(current);
        Season saved = seasonRepository.save(season, "test");
        seasonIds.add(Objects.requireNonNull(saved.getId(), "seasonId"));
        return saved;
    }

    private SeasonWeek week(Season season, int weekNumber, LocalDate begin, LocalDate end) {
        SeasonWeek week = new SeasonWeek();
        week.setSeasonId(Objects.requireNonNull(season.getId(), "seasonId"));
        week.setWeekNumber(weekNumber);
        week.setBeginDate(begin.toString());
        week.setEndDate(end.toString());
        SeasonWeek saved = seasonWeekRepository.save(week, "test");
        weekIds.add(Objects.requireNonNull(saved.getId(), "weekId"));
        return saved;
    }

    private Venue venue() {
        Venue venue = new Venue();
        venue.setVenueName("Stadium " + UUID.randomUUID().toString().substring(0, 8));
        venue.setCityState("Tuscaloosa, AL");
        Venue saved = venueRepository.save(venue, "test");
        venueIds.add(Objects.requireNonNull(saved.getId(), "venueId"));
        return saved;
    }

    private Team team(String name, Venue home) {
        Team team = new Team();
        team.setTeamName(name + " " + UUID.randomUUID().toString().substring(0, 8));
        team.setSquadName("Squad");
        team.setConferenceMember(Boolean.TRUE);
        team.setHomeVenueId(Objects.requireNonNull(home.getId(), "venueId"));
        team.setHomeVenue(new VenueSnapshot(home.getId(), home.getVenueName(), home.getCityState()));
        Team saved = teamRepository.save(team, "test");
        teamIds.add(Objects.requireNonNull(saved.getId(), "teamId"));
        return saved;
    }

    private Matchup matchup(Slate slate, LocalDate date, Long cfbdGameId, Integer weekNumber, Integer homeScore,
            Integer awayScore) {
        Team home = slate.home();
        Team away = slate.away();
        Venue venue = slate.venue();
        Matchup matchup = new Matchup();
        matchup.setSeasonId(Objects.requireNonNull(slate.season().getId(), "seasonId"));
        matchup.setSeasonWeekId(Objects.requireNonNull(slate.week().getId(), "weekId"));
        matchup.setWeekNumber(weekNumber == null ? slate.week().getWeekNumber() : weekNumber);
        matchup.setMatchupDate(date.toString());
        matchup.setHomeTeamId(Objects.requireNonNull(home.getId(), "homeTeamId"));
        matchup.setAwayTeamId(Objects.requireNonNull(away.getId(), "awayTeamId"));
        matchup.setHomeTeam(new TeamSquadSnapshot(home.getId(), home.getTeamName(), home.getSquadName()));
        matchup.setAwayTeam(new TeamSquadSnapshot(away.getId(), away.getTeamName(), away.getSquadName()));
        matchup.setHomeTeamScore(homeScore);
        matchup.setAwayTeamScore(awayScore);
        if (homeScore != null && awayScore != null && !homeScore.equals(awayScore)) {
            matchup.setWinningTeamId(homeScore.compareTo(awayScore) > 0 ? home.getId() : away.getId());
        }
        matchup.setVenueId(Objects.requireNonNull(venue.getId(), "venueId"));
        matchup.setVenue(new VenueSnapshot(venue.getId(), venue.getVenueName(), venue.getCityState()));
        matchup.setCfbdGameId(cfbdGameId);
        Matchup saved = matchupRepository.save(matchup, "test");
        matchupIds.add(Objects.requireNonNull(saved.getId(), "matchupId"));
        return saved;
    }

    private String player() {
        String uid = "player-" + UUID.randomUUID();
        User user = new User();
        user.setId(uid);
        user.setUid(uid);
        user.setEmailAddr(uid + "@example.com");
        user.setFirstName("Test");
        user.setLastName("Player");
        user.setNickName("Picker");
        user.setThemeId("light");
        user.setRoles(List.of(RoleClaims.PLAYER));
        userRepository.save(user, "test");
        userIds.add(uid);
        return uid;
    }

    private void pick(String uid, Slate slate, Matchup matchup, String teamId, int rank) {
        Pick pick = new Pick();
        pick.setUserId(uid);
        pick.setMatchupId(Objects.requireNonNull(matchup.getId(), "matchupId"));
        pick.setSeasonId(Objects.requireNonNull(slate.season().getId(), "seasonId"));
        pick.setSeasonWeekId(Objects.requireNonNull(slate.week().getId(), "weekId"));
        pick.setPickedTeamId(teamId);
        pick.setRank(rank);
        Pick saved = pickRepository.save(pick, "test");
        pickIds.add(Objects.requireNonNull(saved.getId(), "pickId"));
    }

    private Cookie cookie(String uid) {
        return new Cookie(SessionCookies.NAME,
                firebase.issueCookie(new VerifiedIdentity(uid, uid + "@example.com", "Test Player", "Test", "Player",
                        "google.com", RoleClaims.forRoles(List.of(RoleClaims.PLAYER)))));
    }

    private Matchup reload(Matchup matchup) {
        String id = Objects.requireNonNull(matchup.getId(), "matchupId");
        return matchupRepository.findById(id).orElseThrow();
    }

    private JsonNode main(Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/game/main").cookie(cookie)).andExpect(status().isOk()).andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode leaderboard(Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/game/leaderboard").cookie(cookie)).andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private static JsonNode matchupNode(JsonNode main, String matchupId) {
        for (JsonNode week : main.path("weeks")) {
            for (JsonNode matchup : week.path("matchups")) {
                if (matchupId.equals(matchup.path("matchupId").asString())) {
                    return matchup;
                }
            }
        }
        throw new AssertionError("missing matchup " + matchupId);
    }

    private static JsonNode standing(JsonNode board, String uid) {
        for (JsonNode row : board.path("standings")) {
            if (uid.equals(row.path("uid").asString())) {
                return row;
            }
        }
        throw new AssertionError("missing standing " + uid);
    }

    private static long nextGameId() {
        nextGameId++;
        return nextGameId;
    }

    private static void pin(LocalDate date) {
        PinnedClockConfig.NOW.set(date.atTime(16, 0).atZone(PickScoring.LEAGUE_ZONE).toInstant());
    }

    private static RequestPostProcessor loopback() {
        return request -> {
            request.setRemoteAddr("127.0.0.1");
            return request;
        };
    }

    private record Slate(Season season, SeasonWeek week, Team home, Team away, Venue venue) {
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
