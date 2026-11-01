package com.curleesoft.pickem.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.curleesoft.pickem.backend.cfbd.CfbdCalendarWeek;
import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.cfbd.CfbdClientException;
import com.curleesoft.pickem.backend.cfbd.CfbdGame;
import com.curleesoft.pickem.backend.cfbd.CfbdVenue;
import com.curleesoft.pickem.backend.model.ImportJob;
import com.curleesoft.pickem.backend.model.ImportJobSkip;
import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.model.Rivalry;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.ImportJobRepository;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.ImportActorAuthentication;
import com.curleesoft.pickem.backend.seed.SeedCatalog;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;

@SpringBootTest
class CfbdImportServiceTests extends FirestoreEmulatorSupport {

    @Autowired
    private CfbdImportService cfbdImportService;

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
    private MatchupService matchupService;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private PickRepository pickRepository;

    @Autowired
    private RivalryRepository rivalryRepository;

    @MockitoBean
    private CfbdClient cfbdClient;

    @MockitoBean
    private CfbdPause cfbdPause;

    private final List<String> seasonIds = new ArrayList<>();

    private final List<String> jobIds = new ArrayList<>();

    private final List<String> pickIds = new ArrayList<>();

    private final List<String> rivalryIds = new ArrayList<>();

    private String token;

    private boolean unknownExisted;

    @BeforeEach
    void isolate() {
        token = UUID.randomUUID().toString().substring(0, 8);
        unknownExisted = venueRepository.findById(SeedCatalog.UNKNOWN_VENUE_ID).isPresent();
        lenient().when(cfbdClient.venues()).thenReturn(List.of());
    }

    @AfterEach
    void deleteFixtures() {
        for (String pickId : pickIds) {
            pickRepository.delete(pickId);
        }
        for (String rivalryId : rivalryIds) {
            rivalryRepository.delete(rivalryId);
        }
        for (String seasonId : seasonIds) {
            String parentId = Objects.requireNonNull(seasonId, "seasonId");
            for (Matchup matchup : matchups(parentId)) {
                matchupRepository.delete(Objects.requireNonNull(matchup.getId(), "matchupId"));
            }
            for (SeasonWeek week : seasonWeekRepository.findAll()) {
                if (parentId.equals(week.getSeasonId()) && week.getId() != null) {
                    seasonWeekRepository.delete(week.getId());
                }
            }
            seasonRepository.delete(parentId);
        }
        for (Team team : teamRepository.findAll()) {
            if (team.getTeamName() != null && team.getTeamName().contains(token) && team.getId() != null) {
                teamRepository.delete(team.getId());
            }
        }
        for (Venue venue : venueRepository.findAll()) {
            boolean named = venue.getVenueName() != null && venue.getVenueName().contains(token);
            boolean ours = venue.getCfbdVenueId() != null && venue.getCfbdVenueId().longValue() >= 800_000_000L;
            if ((named || ours) && venue.getId() != null) {
                venueRepository.delete(venue.getId());
            }
        }
        if (!unknownExisted) {
            venueRepository.delete(SeedCatalog.UNKNOWN_VENUE_ID);
        }
        for (String jobId : jobIds) {
            importJobRepository.delete(jobId);
        }
        seasonIds.clear();
        jobIds.clear();
        pickIds.clear();
        rivalryIds.clear();
    }

    @Test
    void importWritesCalendarWeeksOpponentsAndMatchupsWithoutDuplicatingThem() {
        int year = uniqueYear();
        LocalDate monday = firstMondayInAugust(year);
        LocalDate sunday = monday.plusDays(6);
        String seasonId = createSeason(year);
        Venue home = venue("Bryant " + token, "Tuscaloosa, AL", null);
        Team alabama = conferenceTeam("Alabama " + token, "Crimson Tide " + token, 800_000_001L, home);
        long jacketsId = 800_000_002L;
        long mocsId = 800_000_003L;
        long furmanId = 800_000_004L;
        long doddId = 800_000_721L;
        long finleyId = 800_000_888L;
        String jackets = "Jackets " + token;
        String mocs = "Mocs " + token;
        String furman = "Furman " + token;
        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        String easternKickoff = sunday.plusDays(1) + "T03:30:00.000Z";
        CfbdGame atDodd = game(910_001L, year, 1, easternKickoff, "Bobby Dodd " + token, doddId, alabama.getCfbdTeamId(),
                alabama.getTeamName(), jacketsId, jackets);
        CfbdGame jacketsHome = game(910_005L, year, 1, monday + "T18:00:00.000Z", "Bobby Dodd " + token, doddId,
                jacketsId, jackets, alabama.getCfbdTeamId(), alabama.getTeamName());
        CfbdGame atFinley = game(910_002L, year, 1, monday + "T18:00:00.000Z", "Finley " + token, finleyId, mocsId, mocs,
                alabama.getCfbdTeamId(), alabama.getTeamName());
        CfbdGame noVenue = game(910_003L, year, 1, monday + "T20:00:00.000Z", null, null, alabama.getCfbdTeamId(),
                alabama.getTeamName(), furmanId, furman);
        CfbdGame unmapped = game(910_004L, year, 4, monday + "T18:00:00.000Z", null, null, alabama.getCfbdTeamId(),
                alabama.getTeamName(), furmanId, furman);
        CfbdGame earlyKickoff = game(910_006L, year, 1, monday + "T02:00:00.000Z", null, null, alabama.getCfbdTeamId(),
                alabama.getTeamName(), furmanId, furman);
        when(cfbdClient.calendar(year)).thenReturn(List.of(week));
        when(cfbdClient.games(eq(year), isNull(), eq("regular")))
                .thenReturn(List.of(atDodd, jacketsHome, atFinley, noVenue, unmapped, earlyKickoff, atDodd));

        ImportJob queued = cfbdImportService.enqueue(seasonId, null);
        jobIds.add(Objects.requireNonNull(queued.getId(), "jobId"));
        verifyNoInteractions(cfbdClient);
        assertThat(queued.getStatus()).isEqualTo(ImportJob.QUEUED);

        cfbdImportService.execute(queued.getId());

        ImportJob done = cfbdImportService.get(queued.getId());
        assertThat(done.getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(done.getError()).isNull();
        assertThat(done.getSeasonType()).isEqualTo("regular");
        assertThat(done.getWeeksCreated()).isEqualTo(1);
        assertThat(done.getTeamsCreated()).isEqualTo(3);
        assertThat(done.getVenuesCreated()).isEqualTo(2);
        assertThat(done.getMatchupsCreated()).isEqualTo(5);
        assertThat(done.getSkipped()).extracting(ImportJobSkip::getCfbdGameId).containsExactly(910_004L);

        SeasonWeek storedWeek = onlyWeek(seasonId);
        assertThat(storedWeek.getBeginDate()).isEqualTo(monday.minusDays(1).toString());
        assertThat(LocalDate.parse(storedWeek.getBeginDate()).getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(storedWeek.getEndDate()).isEqualTo(sunday.toString());
        assertThat(storedWeek.getCreateUser()).isEqualTo(ImportActorAuthentication.ACTOR);

        Team createdJackets = teamByCfbd(jacketsId);
        assertThat(createdJackets.getConferenceMember()).isFalse();
        assertThat(createdJackets.getSquadName()).isEqualTo(jackets);
        assertThat(createdJackets.getTeamName()).isEqualTo(jackets);
        assertThat(createdJackets.getHomeVenue()).isNotNull();
        assertThat(createdJackets.getCreateUser()).isEqualTo(ImportActorAuthentication.ACTOR);

        Team createdMocs = teamByCfbd(mocsId);
        Venue finley = venueByCfbd(finleyId);
        assertThat(createdMocs.getConferenceMember()).isFalse();
        assertThat(createdMocs.getSquadName()).isEqualTo(mocs);
        assertThat(createdMocs.getHomeVenueId()).isEqualTo(finley.getId());
        assertThat(createdMocs.getHomeVenue().getName()).isEqualTo("Finley " + token);
        assertThat(createdMocs.getHomeVenue().getCityState()).isEqualTo("Unknown");

        Team createdFurman = teamByCfbd(furmanId);
        assertThat(createdFurman.getHomeVenueId()).isEqualTo(SeedCatalog.UNKNOWN_VENUE_ID);
        assertThat(createdFurman.getHomeVenue()).isNotNull();

        Venue dodd = venueByCfbd(doddId);
        assertThat(createdJackets.getHomeVenueId()).isEqualTo(dodd.getId());
        assertThat(createdJackets.getHomeVenue().getId()).isEqualTo(dodd.getId());
        Matchup doddGame = matchupByGame(910_001L);
        assertThat(doddGame.getSeasonWeekId()).isEqualTo(storedWeek.getId());
        assertThat(doddGame.getMatchupDate()).isEqualTo(sunday.toString());
        assertThat(doddGame.getHomeTeamId()).isEqualTo(alabama.getId());
        assertThat(doddGame.getAwayTeamId()).isEqualTo(createdJackets.getId());
        assertThat(doddGame.getVenueId()).isEqualTo(dodd.getId());
        assertThat(doddGame.getHomeTeam().getName()).isEqualTo(alabama.getTeamName());
        assertThat(doddGame.getAwayTeam().getSquad()).isEqualTo(jackets);
        assertThat(doddGame.getHomeTeamScore()).isNull();
        assertThat(doddGame.getAwayTeamScore()).isNull();

        Matchup finleyGame = matchupByGame(910_002L);
        assertThat(finleyGame.getHomeTeamId()).isEqualTo(createdMocs.getId());
        assertThat(finleyGame.getVenueId()).isEqualTo(finley.getId());

        Matchup earlyGame = matchupByGame(910_006L);
        assertThat(earlyGame.getMatchupDate()).isEqualTo(monday.minusDays(1).toString());
        assertThat(earlyGame.getSeasonWeekId()).isEqualTo(storedWeek.getId());

        Matchup campusGame = matchupByGame(910_003L);
        assertThat(campusGame.getVenueId()).isEqualTo(home.getId());
        assertThat(campusGame.getAwayTeamId()).isEqualTo(createdFurman.getId());

        campusGame.setHomeTeamScore(21);
        campusGame.setAwayTeamScore(14);
        matchupService.update(Objects.requireNonNull(campusGame.getId(), "matchupId"), campusGame);

        cfbdImportService.execute(queued.getId());

        ImportJob again = cfbdImportService.get(queued.getId());
        assertThat(again.getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(again.getWeeksCreated()).isZero();
        assertThat(again.getWeeksUpdated()).isEqualTo(1);
        assertThat(again.getTeamsCreated()).isZero();
        assertThat(again.getVenuesCreated()).isZero();
        assertThat(again.getMatchupsCreated()).isZero();
        assertThat(again.getMatchupsUpdated()).isEqualTo(5);
        assertThat(weeks(seasonId)).hasSize(1);
        assertThat(teamsNamed(jackets)).hasSize(1);
        assertThat(teamsNamed(mocs)).hasSize(1);
        assertThat(teamsNamed(furman)).hasSize(1);
        Matchup preserved = matchupByGame(910_003L);
        assertThat(preserved.getId()).isEqualTo(campusGame.getId());
        assertThat(preserved.getHomeTeamScore()).isEqualTo(21);
        assertThat(preserved.getAwayTeamScore()).isEqualTo(14);
        assertThat(preserved.getWinningTeamId()).isEqualTo(alabama.getId());
        verify(cfbdPause, never()).pause(anyInt());
    }

    @Test
    void transientFailuresAreRetriedAndAFinalFailureIsStoredOnTheJob() {
        int year = uniqueYear();
        String seasonId = createSeason(year);
        LocalDate monday = firstMondayInAugust(year);
        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                monday.plusDays(6) + "T23:59:59.000Z");
        when(cfbdClient.calendar(year)).thenThrow(new CfbdClientException(429, CfbdClientException.RATE_LIMITED))
                .thenThrow(new CfbdClientException(503, CfbdClientException.UNAVAILABLE)).thenReturn(List.of(week));
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of());

        ImportJob queued = cfbdImportService.enqueue(seasonId, " regular ");
        jobIds.add(Objects.requireNonNull(queued.getId(), "jobId"));
        cfbdImportService.execute(queued.getId());

        assertThat(cfbdImportService.get(queued.getId()).getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(onlyWeek(seasonId).getWeekNumber()).isEqualTo(1);
        verify(cfbdClient, times(3)).calendar(year);
        verify(cfbdPause).pause(1);
        verify(cfbdPause).pause(2);

        int failedYear = uniqueYear();
        String failedSeason = createSeason(failedYear);
        when(cfbdClient.calendar(failedYear)).thenThrow(new CfbdClientException(503, CfbdClientException.UNAVAILABLE));
        ImportJob failedJob = cfbdImportService.enqueue(failedSeason, null);
        jobIds.add(Objects.requireNonNull(failedJob.getId(), "jobId"));
        assertThatThrownBy(() -> cfbdImportService.execute(failedJob.getId())).isInstanceOf(CfbdClientException.class)
                .hasMessage(CfbdClientException.UNAVAILABLE);
        ImportJob failed = cfbdImportService.get(failedJob.getId());
        assertThat(failed.getStatus()).isEqualTo(ImportJob.FAILED);
        assertThat(failed.getError()).isEqualTo(CfbdClientException.UNAVAILABLE);
        verify(cfbdClient, times(3)).calendar(failedYear);

        int rejectedYear = uniqueYear();
        String rejectedSeason = createSeason(rejectedYear);
        when(cfbdClient.calendar(rejectedYear)).thenThrow(new CfbdClientException(401, CfbdClientException.REJECTED_KEY));
        ImportJob rejectedJob = cfbdImportService.enqueue(rejectedSeason, null);
        jobIds.add(Objects.requireNonNull(rejectedJob.getId(), "jobId"));
        assertThatThrownBy(() -> cfbdImportService.execute(rejectedJob.getId())).isInstanceOf(CfbdClientException.class);
        assertThat(cfbdImportService.get(rejectedJob.getId()).getError()).isEqualTo(CfbdClientException.REJECTED_KEY);
        verify(cfbdClient, times(1)).calendar(rejectedYear);
        verify(cfbdPause, times(4)).pause(anyInt());
    }

    @Test
    void postseasonImportKeepsOnlyThatCalendar() {
        int year = uniqueYear();
        String seasonId = createSeason(year);
        LocalDate monday = firstMondayInAugust(year);
        LocalDate bowl = LocalDate.of(year, 12, 15);
        CfbdCalendarWeek regular = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                monday.plusDays(6) + "T23:59:59.000Z");
        CfbdCalendarWeek postseason = new CfbdCalendarWeek(year, 1, "postseason", bowl + "T00:00:00.000Z",
                bowl.plusDays(6) + "T23:59:59.000Z");
        when(cfbdClient.calendar(year)).thenReturn(List.of(regular, postseason));
        when(cfbdClient.games(eq(year), isNull(), eq("postseason"))).thenReturn(List.of());

        ImportJob job = cfbdImportService.enqueue(seasonId, "postseason");
        jobIds.add(Objects.requireNonNull(job.getId(), "jobId"));
        cfbdImportService.execute(job.getId());

        SeasonWeek stored = onlyWeek(seasonId);
        assertThat(stored.getBeginDate()).isEqualTo(bowl.minusDays(1).toString());
        assertThat(stored.getWeekNumber()).isEqualTo(CfbdImportService.POSTSEASON_WEEK_BASE + 1);
        verify(cfbdClient).games(year, null, "postseason");
    }

    @Test
    void postseasonImportDoesNotRewriteRegularWeekOne() {
        int year = uniqueYear();
        String seasonId = createSeason(year);
        LocalDate monday = firstMondayInAugust(year);
        LocalDate sunday = monday.plusDays(6);
        LocalDate bowl = LocalDate.of(year, 12, 15);
        Venue home = venue("Bryant " + token, "Tuscaloosa, AL", null);
        Team alabama = conferenceTeam("Alabama " + token, "Tide " + token, 800_000_011L, home);
        long guestId = 800_000_012L;
        String guest = "Guest " + token;
        CfbdCalendarWeek regular = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        CfbdCalendarWeek postseason = new CfbdCalendarWeek(year, 1, "postseason", bowl + "T00:00:00.000Z",
                bowl.plusDays(6) + "T23:59:59.000Z");
        CfbdGame opener = game(920_001L, year, 1, monday + "T18:00:00.000Z", null, null, alabama.getCfbdTeamId(),
                alabama.getTeamName(), guestId, guest);
        CfbdGame bowlGame = game(920_002L, year, 1, "postseason", bowl + "T18:00:00.000Z", "Bowl " + token, 800_000_777L,
                alabama.getCfbdTeamId(), alabama.getTeamName(), guestId, guest);
        when(cfbdClient.calendar(year)).thenReturn(List.of(regular, postseason));
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of(opener));
        when(cfbdClient.games(eq(year), isNull(), eq("postseason"))).thenReturn(List.of(bowlGame));

        ImportJob regularJob = cfbdImportService.enqueue(seasonId, "regular");
        jobIds.add(Objects.requireNonNull(regularJob.getId(), "jobId"));
        cfbdImportService.execute(regularJob.getId());
        SeasonWeek opening = onlyWeek(seasonId);
        Matchup openingGame = matchupByGame(920_001L);
        String openingId = Objects.requireNonNull(opening.getId(), "weekId");
        String openingDate = opening.getBeginDate();

        ImportJob bowlJob = cfbdImportService.enqueue(seasonId, "postseason");
        jobIds.add(Objects.requireNonNull(bowlJob.getId(), "jobId"));
        cfbdImportService.execute(bowlJob.getId());

        SeasonWeek stillOpening = null;
        SeasonWeek bowlWeek = null;
        for (SeasonWeek week : weeks(seasonId)) {
            if (Integer.valueOf(1).equals(week.getWeekNumber())) {
                stillOpening = week;
            }
            if (Integer.valueOf(CfbdImportService.POSTSEASON_WEEK_BASE + 1).equals(week.getWeekNumber())) {
                bowlWeek = week;
            }
        }
        SeasonWeek regularWeek = Objects.requireNonNull(stillOpening, "regularWeek");
        SeasonWeek postseasonWeek = Objects.requireNonNull(bowlWeek, "postseasonWeek");
        assertThat(regularWeek.getId()).isEqualTo(openingId);
        assertThat(regularWeek.getBeginDate()).isEqualTo(openingDate);
        assertThat(regularWeek.getEndDate()).isEqualTo(sunday.toString());
        assertThat(matchupByGame(920_001L).getId()).isEqualTo(openingGame.getId());
        assertThat(matchupByGame(920_001L).getSeasonWeekId()).isEqualTo(openingId);
        assertThat(postseasonWeek.getBeginDate()).isEqualTo(bowl.minusDays(1).toString());
        assertThat(matchupByGame(920_002L).getSeasonWeekId()).isEqualTo(postseasonWeek.getId());
        assertThat(teamByCfbd(guestId).getHomeVenueId()).isEqualTo(SeedCatalog.UNKNOWN_VENUE_ID);
    }

    @Test
    void missingConferenceTeamMatchesSeedIdsAndSecName() {
        Map<Long, Team> none = Map.of();
        Team present = new Team();
        assertThat(CfbdImportService.missingConferenceTeam(333L, null, none)).isTrue();
        assertThat(CfbdImportService.missingConferenceTeam(333L, null, Map.of(Long.valueOf(333L), present))).isFalse();
        assertThat(CfbdImportService.missingConferenceTeam(810_000_777L, " sec ", none)).isTrue();
        assertThat(CfbdImportService.missingConferenceTeam(810_000_777L, "ACC", none)).isFalse();
        assertThat(CfbdImportService.missingConferenceTeam(0L, null, none)).isFalse();
        assertThat(CfbdImportService.cityState("Atlanta", "GA")).isEqualTo("Atlanta, GA");
        assertThat(CfbdImportService.cityState(" Atlanta ", null)).isEqualTo("Atlanta");
        assertThat(CfbdImportService.cityState(null, " GA ")).isEqualTo("GA");
        assertThat(CfbdImportService.cityState(" ", " ")).isEqualTo("Unknown");
    }

    @Test
    void importKeepsOneRowPerSecGameAndSkipsAMissingConferenceTeam() {
        int year = uniqueYear();
        LocalDate monday = firstMondayInAugust(year);
        LocalDate sunday = monday.plusDays(6);
        String seasonId = createSeason(year);
        Venue bryant = venue("Bryant " + token, "Tuscaloosa, AL", null);
        Venue sanford = venue("Sanford " + token, "Athens, GA", null);
        List<Team> members = new ArrayList<>();
        members.add(conferenceTeam("Alabama " + token, "Crimson Tide " + token, 810_000_001L, bryant));
        members.add(conferenceTeam("Auburn " + token, "Tigers " + token, 810_000_002L, sanford));
        for (int index = 3; index <= 16; index++) {
            members.add(conferenceTeam("Club " + index + " " + token, "Squad " + index, 810_000_000L + index, bryant));
        }
        Team alabama = members.get(0);
        Team auburn = members.get(1);
        Rivalry rivalry = new Rivalry();
        rivalry.setRivalryName("Iron " + token);
        rivalry.setTeam1Id(alabama.getId());
        rivalry.setTeam2Id(auburn.getId());
        Rivalry savedRivalry = rivalryRepository.save(rivalry, "test");
        rivalryIds.add(Objects.requireNonNull(savedRivalry.getId(), "rivalryId"));

        long mercedesId = 810_000_500L;
        long domeId = 810_000_501L;
        long furmanId = 810_000_050L;
        String furman = "Furman " + token;
        String kickoff = monday + "T18:00:00.000Z";
        String later = monday.plusDays(2) + "T18:00:00.000Z";
        List<CfbdGame> opening = new ArrayList<>();
        List<CfbdGame> rerun = new ArrayList<>();
        for (int index = 0; index < members.size(); index += 2) {
            Team home = Objects.requireNonNull(members.get(index), "home");
            Team away = Objects.requireNonNull(members.get(index + 1), "away");
            long gameId = 930_001L + (index / 2);
            if (index == 0) {
                opening.add(game(gameId, year, 1, kickoff, false, "Mercedes " + token, Long.valueOf(mercedesId), home,
                        away, null, null, true, null, null));
                rerun.add(game(gameId, year, 1, later, false, "Dome " + token, Long.valueOf(domeId), home, away, null,
                        null, true, null, null));
            } else if (index == 2) {
                opening.add(game(gameId, year, 1, kickoff, false, null, null, home, away, null, null, false, null,
                        null));
                rerun.add(game(gameId, year, 1, kickoff, true, null, null, home, away, Integer.valueOf(10),
                        Integer.valueOf(10), false, null, null));
            } else {
                CfbdGame row = game(gameId, year, 1, kickoff, false, null, null, home, away, null, null, false, null,
                        null);
                opening.add(row);
                rerun.add(row);
            }
        }
        opening.add(opening.get(0));
        rerun.add(rerun.get(0));
        Long alabamaId = Objects.requireNonNull(alabama.getCfbdTeamId(), "alabamaId");
        opening.add(game(930_009L, year, 1, "regular", monday + "T20:00:00.000Z", true, null, null,
                alabamaId.longValue(), alabama.getTeamName(), Integer.valueOf(31), furmanId, furman,
                Integer.valueOf(17), false, null, null));
        rerun.add(game(930_009L, year, 1, "regular", monday + "T20:00:00.000Z", true, null, null,
                alabamaId.longValue(), alabama.getTeamName(), Integer.valueOf(28), furmanId, furman,
                Integer.valueOf(14), false, null, null));
        Long auburnId = Objects.requireNonNull(auburn.getCfbdTeamId(), "auburnId");
        long showcaseId = 810_000_060L;
        String showcase = "Showcase " + token;
        opening.add(game(930_010L, year, 1, "regular", kickoff, false, "Mercedes " + token, Long.valueOf(mercedesId),
                showcaseId, showcase, null, alabamaId.longValue(), alabama.getTeamName(), null, true, null, null));
        rerun.add(game(930_010L, year, 1, "regular", kickoff, false, "Mercedes " + token, Long.valueOf(mercedesId),
                showcaseId, showcase, null, alabamaId.longValue(), alabama.getTeamName(), null, true, null, null));
        opening.add(game(930_099L, year, 1, "regular", kickoff, false, null, null, 810_000_777L, "Ghost " + token, null,
                auburnId.longValue(), auburn.getTeamName(), null, false, "SEC", null));
        rerun.add(opening.get(opening.size() - 1));

        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        when(cfbdClient.calendar(year)).thenReturn(List.of(week));
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(opening, rerun);
        when(cfbdClient.venues()).thenReturn(List.of(new CfbdVenue(mercedesId, "Mercedes " + token, "Atlanta", "GA")));

        ImportJob queued = cfbdImportService.enqueue(seasonId, null);
        jobIds.add(Objects.requireNonNull(queued.getId(), "jobId"));
        assertThat(queued.getSeasonYear()).isEqualTo(Integer.valueOf(year));
        cfbdImportService.execute(queued.getId());

        ImportJob done = cfbdImportService.get(queued.getId());
        assertThat(done.getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(done.getMatchupsCreated()).isEqualTo(10);
        assertThat(done.getTeamsCreated()).isEqualTo(2);
        assertThat(done.getVenuesCreated()).isEqualTo(1);
        assertThat(done.getSkipped()).hasSize(1);
        ImportJobSkip skipped = Objects.requireNonNull(done.getSkipped().get(0), "skip");
        assertThat(skipped.getCfbdGameId()).isEqualTo(Long.valueOf(930_099L));
        assertThat(skipped.getReason()).isEqualTo(CfbdImportService.MISSING_CONFERENCE_TEAM);
        assertThat(matchups(seasonId)).hasSize(10);
        assertThat(teamsNamed("Ghost " + token)).isEmpty();
        assertThat(teamByCfbd(furmanId).getConferenceMember()).isFalse();

        List<String> seen = new ArrayList<>();
        for (int number = 0; number < 8; number++) {
            Matchup row = matchupByGame(930_001L + number);
            seen.add(row.getHomeTeamId());
            seen.add(row.getAwayTeamId());
            if (number > 1) {
                assertThat(row.getHomeTeamScore()).isNull();
                assertThat(row.getAwayTeamScore()).isNull();
            }
        }
        assertThat(seen).hasSize(16);
        for (Team member : members) {
            assertThat(seen).contains(member.getId());
        }

        Matchup ironBowl = matchupByGame(930_001L);
        String ironBowlId = Objects.requireNonNull(ironBowl.getId(), "ironBowlId");
        Venue mercedes = venueByCfbd(mercedesId);
        assertThat(mercedes.getCityState()).isEqualTo("Atlanta, GA");
        assertThat(ironBowl.getVenueId()).isEqualTo(mercedes.getId());
        assertThat(ironBowl.getVenueId()).isNotEqualTo(bryant.getId());
        assertThat(ironBowl.getRivalryName()).isEqualTo("Iron " + token);
        assertThat(ironBowl.getHomeTeamScore()).isNull();

        Matchup decided = matchupByGame(930_009L);
        assertThat(decided.getHomeTeamScore()).isEqualTo(31);
        assertThat(decided.getAwayTeamScore()).isEqualTo(17);
        assertThat(decided.getWinningTeamId()).isEqualTo(alabama.getId());

        Matchup preserved = matchupByGame(930_002L);
        preserved.setHomeTeamScore(7);
        preserved.setAwayTeamScore(3);
        matchupService.update(Objects.requireNonNull(preserved.getId(), "preservedId"), preserved);
        Pick pick = new Pick();
        String pickId = PickRepository.documentId("player-" + token, preserved.getId());
        pick.setId(pickId);
        pick.setUserId("player-" + token);
        pick.setMatchupId(preserved.getId());
        pick.setSeasonId(seasonId);
        pick.setSeasonWeekId(preserved.getSeasonWeekId());
        pick.setPickedTeamId(preserved.getHomeTeamId());
        pick.setRank(Integer.valueOf(3));
        pickRepository.save(pick, "test");
        pickIds.add(pickId);

        cfbdImportService.execute(queued.getId());

        ImportJob again = cfbdImportService.get(queued.getId());
        assertThat(again.getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(again.getMatchupsCreated()).isZero();
        assertThat(again.getMatchupsUpdated()).isEqualTo(10);
        assertThat(again.getTeamsCreated()).isZero();
        assertThat(again.getVenuesCreated()).isEqualTo(1);
        assertThat(matchups(seasonId)).hasSize(10);

        Matchup moved = matchupByGame(930_001L);
        assertThat(moved.getId()).isEqualTo(ironBowlId);
        assertThat(moved.getMatchupDate()).isEqualTo(monday.plusDays(2).toString());
        assertThat(moved.getVenueId()).isEqualTo(venueByCfbd(domeId).getId());
        assertThat(venueByCfbd(domeId).getCityState()).isEqualTo("Unknown");
        assertThat(moved.getRivalryName()).isEqualTo("Iron " + token);
        assertThat(moved.getHomeTeamScore()).isNull();

        Matchup tied = matchupByGame(930_002L);
        assertThat(tied.getHomeTeamScore()).isEqualTo(7);
        assertThat(tied.getAwayTeamScore()).isEqualTo(3);
        assertThat(tied.getWinningTeamId()).isEqualTo(tied.getHomeTeamId());

        Matchup rescored = matchupByGame(930_009L);
        assertThat(rescored.getId()).isEqualTo(decided.getId());
        assertThat(rescored.getHomeTeamScore()).isEqualTo(28);
        assertThat(rescored.getAwayTeamScore()).isEqualTo(14);
        assertThat(rescored.getWinningTeamId()).isEqualTo(alabama.getId());

        Pick storedPick = Objects.requireNonNull(pickRepository.findById(pickId).orElseThrow(), "pick");
        assertThat(storedPick.getRank()).isEqualTo(3);
        assertThat(storedPick.getPickedTeamId()).isEqualTo(preserved.getHomeTeamId());
        assertThat(pickRepository.findByUserId("player-" + token)).hasSize(1);
        assertThat(teamsNamed("Ghost " + token)).isEmpty();
        Team createdShowcase = teamByCfbd(showcaseId);
        assertThat(createdShowcase.getConferenceMember()).isFalse();
        assertThat(createdShowcase.getHomeVenueId()).isEqualTo(SeedCatalog.UNKNOWN_VENUE_ID);
        assertThat(matchupByGame(930_010L).getVenueId()).isEqualTo(venueByCfbd(mercedesId).getId());
    }

    @Test
    void venueCatalogFailureDoesNotBlockImportAndKnownStadiumsSkipTheCall() {
        int year = uniqueYear();
        LocalDate monday = firstMondayInAugust(year);
        LocalDate sunday = monday.plusDays(6);
        String seasonId = createSeason(year);
        long knownId = 810_000_610L;
        Venue known = venue("Known " + token, "Tuscaloosa, AL", Long.valueOf(knownId));
        Venue stale = venue("Stale " + token, "Unknown", Long.valueOf(810_000_611L));
        Team alabama = conferenceTeam("Alabama " + token, "Tide " + token, 810_000_612L, known);
        long guestId = 810_000_613L;
        String guest = "Guest " + token;
        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        CfbdGame atKnown = game(940_001L, year, 1, monday + "T18:00:00.000Z", known.getVenueName(), knownId,
                alabama.getCfbdTeamId(), alabama.getTeamName(), guestId, guest);
        when(cfbdClient.calendar(year)).thenReturn(List.of(week));
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of(atKnown));
        lenient().doThrow(new CfbdClientException(503, CfbdClientException.UNAVAILABLE)).when(cfbdClient).venues();

        ImportJob knownOnly = cfbdImportService.enqueue(seasonId, null);
        jobIds.add(Objects.requireNonNull(knownOnly.getId(), "jobId"));
        cfbdImportService.execute(knownOnly.getId());

        assertThat(cfbdImportService.get(knownOnly.getId()).getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(matchupByGame(940_001L).getVenueId()).isEqualTo(known.getId());
        assertThat(known.getCityState()).isEqualTo("Tuscaloosa, AL");
        verify(cfbdClient, never()).venues();

        long freshId = 810_000_614L;
        CfbdGame atFresh = game(940_002L, year, 1, monday + "T20:00:00.000Z", "Fresh " + token, freshId,
                guestId, guest, alabama.getCfbdTeamId().longValue(), alabama.getTeamName());
        CfbdGame atStale = game(940_003L, year, 1, monday + "T21:00:00.000Z", stale.getVenueName(),
                stale.getCfbdVenueId(), alabama.getCfbdTeamId(), alabama.getTeamName(), guestId, guest);
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of(atFresh, atStale));
        doReturn(List.of(new CfbdVenue(810_000_611L, stale.getVenueName(), "Atlanta", "GA"))).when(cfbdClient)
                .venues();

        cfbdImportService.execute(knownOnly.getId());

        assertThat(venueByCfbd(810_000_611L).getCityState()).isEqualTo("Atlanta, GA");
        assertThat(cfbdImportService.get(knownOnly.getId()).getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        Matchup fresh = matchupByGame(940_002L);
        assertThat(fresh.getVenue().getCityState()).isEqualTo("Unknown");
        assertThat(teamByCfbd(guestId).getConferenceMember()).isFalse();
    }

    @Test
    void nullStoredCityIsFilledAndMissingIdsAreNamedBeforeAnEqualTeamSkip() {
        int year = uniqueYear();
        LocalDate monday = firstMondayInAugust(year);
        LocalDate sunday = monday.plusDays(6);
        String seasonId = createSeason(year);
        long blankId = 810_000_700L;
        Venue blank = new Venue();
        blank.setVenueName("Blank " + token);
        blank.setCfbdVenueId(Long.valueOf(blankId));
        blank = venueRepository.save(blank, "test");
        Team alabama = conferenceTeam("Alabama " + token, "Tide " + token, 810_000_701L, blank);
        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        long guestId = 810_000_702L;
        CfbdGame atBlank = game(950_001L, year, 1, monday + "T18:00:00.000Z", blank.getVenueName(), blankId,
                alabama.getCfbdTeamId(), alabama.getTeamName(), guestId, "Guest " + token);
        CfbdGame bothMissing = game(950_002L, year, 1, "regular", monday + "T19:00:00.000Z", false, "Nowhere", null, 0L,
                "Nobody", null, 0L, "Nobody Else", null, false, null, null);
        CfbdGame secMissing = game(950_003L, year, 1, "regular", monday + "T20:00:00.000Z", false, "Nowhere", null, 0L,
                "Ghost", null, 0L, "Visitor", null, false, "SEC", null);
        when(cfbdClient.calendar(year)).thenReturn(List.of(week));
        when(cfbdClient.games(eq(year), isNull(), eq("regular")))
                .thenReturn(List.of(atBlank, bothMissing, secMissing));
        doReturn(List.of(new CfbdVenue(blankId, blank.getVenueName(), "Nashville", "TN"))).when(cfbdClient).venues();

        ImportJob queued = cfbdImportService.enqueue(seasonId, null);
        jobIds.add(Objects.requireNonNull(queued.getId(), "jobId"));
        cfbdImportService.execute(queued.getId());

        ImportJob done = cfbdImportService.get(queued.getId());
        assertThat(done.getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        assertThat(venueByCfbd(blankId).getCityState()).isEqualTo("Nashville, TN");
        assertThat(done.getSkipped()).extracting(ImportJobSkip::getCfbdGameId, ImportJobSkip::getReason)
                .containsExactlyInAnyOrder(tuple(Long.valueOf(950_002L), CfbdImportService.MISSING_TEAM_ID),
                        tuple(Long.valueOf(950_003L), CfbdImportService.MISSING_CONFERENCE_TEAM));
        assertThat(teamsNamed("Nobody")).isEmpty();
        assertThat(teamsNamed("Ghost")).isEmpty();
    }

    @Test
    void rerunMovesScoresWithASwapAndClearsThemWhenTheOpponentChanges() {
        int year = uniqueYear();
        LocalDate monday = firstMondayInAugust(year);
        LocalDate sunday = monday.plusDays(6);
        String seasonId = createSeason(year);
        Venue bryant = venue("Bryant " + token, "Tuscaloosa, AL", null);
        Venue sanford = venue("Sanford " + token, "Athens, GA", null);
        Team alabama = conferenceTeam("Alabama " + token, "Tide " + token, 810_000_801L, bryant);
        Team auburn = conferenceTeam("Auburn " + token, "Tigers " + token, 810_000_802L, sanford);
        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        String start = monday + "T18:00:00.000Z";
        CfbdGame opening = game(960_001L, year, 1, start, false, null, null, alabama, auburn, null, null, false, null,
                null);
        when(cfbdClient.calendar(year)).thenReturn(List.of(week));
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of(opening));

        ImportJob queued = cfbdImportService.enqueue(seasonId, null);
        jobIds.add(Objects.requireNonNull(queued.getId(), "jobId"));
        cfbdImportService.execute(queued.getId());

        Matchup scored = matchupByGame(960_001L);
        String matchupId = Objects.requireNonNull(scored.getId(), "matchupId");
        scored.setHomeTeamScore(21);
        scored.setAwayTeamScore(17);
        matchupService.update(matchupId, scored);

        CfbdGame swapped = game(960_001L, year, 1, start, false, null, null, auburn, alabama, null, null, false, null,
                null);
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of(swapped));
        cfbdImportService.execute(queued.getId());

        Matchup flipped = matchupByGame(960_001L);
        assertThat(flipped.getId()).isEqualTo(matchupId);
        assertThat(flipped.getHomeTeamId()).isEqualTo(auburn.getId());
        assertThat(flipped.getAwayTeamId()).isEqualTo(alabama.getId());
        assertThat(flipped.getHomeTeamScore()).isEqualTo(17);
        assertThat(flipped.getAwayTeamScore()).isEqualTo(21);
        assertThat(flipped.getWinningTeamId()).isEqualTo(alabama.getId());

        long guestId = 810_000_803L;
        CfbdGame replaced = game(960_001L, year, 1, start, null, null, alabama.getCfbdTeamId(), alabama.getTeamName(),
                guestId, "Guest " + token);
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of(replaced));
        cfbdImportService.execute(queued.getId());

        Matchup cleared = matchupByGame(960_001L);
        assertThat(cleared.getId()).isEqualTo(matchupId);
        assertThat(cleared.getAwayTeamId()).isEqualTo(teamByCfbd(guestId).getId());
        assertThat(cleared.getHomeTeamScore()).isNull();
        assertThat(cleared.getAwayTeamScore()).isNull();
        assertThat(cleared.getWinningTeamId()).isNull();
        assertThat(teamByCfbd(guestId).getConferenceMember()).isFalse();
    }

    private String createSeason(int year) {
        Season season = new Season();
        season.setSeason(Integer.toString(year));
        season.setBeginDate(year + "-08-01");
        season.setEndDate(year + "-12-31");
        season.setCurrent(false);
        Season saved = seasonRepository.save(season, "test");
        String id = Objects.requireNonNull(saved.getId(), "seasonId");
        seasonIds.add(id);
        return id;
    }

    private Venue venue(String name, String cityState, Long cfbdVenueId) {
        Venue venue = new Venue();
        venue.setVenueName(name);
        venue.setCityState(cityState);
        venue.setCfbdVenueId(cfbdVenueId);
        return venueRepository.save(venue, "test");
    }

    private Team conferenceTeam(String name, String squad, long cfbdTeamId, Venue home) {
        Team team = new Team();
        team.setTeamName(name);
        team.setSquadName(squad);
        team.setConferenceMember(Boolean.TRUE);
        team.setHomeVenueId(home.getId());
        team.setHomeVenue(new VenueSnapshot(home.getId(), home.getVenueName(), home.getCityState()));
        team.setCfbdTeamId(cfbdTeamId);
        return teamRepository.save(team, "test");
    }

    private static CfbdGame game(long id, int year, int week, String start, String venue, Long venueId, Long homeId,
            String homeName, long awayId, String awayName) {
        return game(id, year, week, "regular", start, venue, venueId, homeId, homeName, awayId, awayName);
    }

    private static CfbdGame game(long id, int year, int week, String seasonType, String start, String venue,
            Long venueId, Long homeId, String homeName, long awayId, String awayName) {
        Long home = Objects.requireNonNull(homeId, "homeId");
        String type = Objects.requireNonNull(seasonType, "seasonType");
        return game(id, year, week, type, start, false, venue, venueId, home.longValue(), homeName, null, awayId,
                awayName, null, false, null, null);
    }

    private static CfbdGame game(long id, int year, int week, String seasonType, String start, boolean completed,
            String venue, Long venueId, long homeId, String homeName, Integer homePoints, long awayId, String awayName,
            Integer awayPoints, boolean neutralSite, String homeConference, String awayConference) {
        String type = Objects.requireNonNull(seasonType, "seasonType");
        return new CfbdGame(id, year, week, type, start, completed, venue, venueId, homeId, homeName, homePoints,
                awayId, awayName, awayPoints, neutralSite, homeConference, awayConference);
    }

    private static CfbdGame game(long id, int year, int week, String start, boolean completed, String venue,
            Long venueId, Team home, Team away, Integer homePoints, Integer awayPoints, boolean neutralSite,
            String homeConference, String awayConference) {
        Team homeTeam = Objects.requireNonNull(home, "home");
        Team awayTeam = Objects.requireNonNull(away, "away");
        Long homeId = Objects.requireNonNull(homeTeam.getCfbdTeamId(), "homeId");
        Long awayId = Objects.requireNonNull(awayTeam.getCfbdTeamId(), "awayId");
        return game(id, year, week, "regular", start, completed, venue, venueId, homeId.longValue(),
                homeTeam.getTeamName(), homePoints, awayId.longValue(), awayTeam.getTeamName(), awayPoints, neutralSite,
                homeConference, awayConference);
    }

    private SeasonWeek onlyWeek(String seasonId) {
        List<SeasonWeek> found = weeks(seasonId);
        assertThat(found).hasSize(1);
        return found.get(0);
    }

    private List<SeasonWeek> weeks(String seasonId) {
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        List<SeasonWeek> found = new ArrayList<>();
        for (SeasonWeek week : seasonWeekRepository.findAll()) {
            if (parentId.equals(week.getSeasonId())) {
                found.add(week);
            }
        }
        return found;
    }

    private List<Matchup> matchups(String seasonId) {
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        return matchupRepository.query(collection -> {
            CollectionReference matchups = Objects.requireNonNull(collection, "collection");
            Query query = Objects.requireNonNull(matchups.whereEqualTo("seasonId", parentId), "query");
            return query;
        });
    }

    private Matchup matchupByGame(long cfbdGameId) {
        Long externalId = Long.valueOf(cfbdGameId);
        List<Matchup> found = matchupRepository.query(collection -> {
            CollectionReference matchups = Objects.requireNonNull(collection, "collection");
            Query query = Objects.requireNonNull(matchups.whereEqualTo("cfbdGameId", externalId), "query");
            return query;
        });
        assertThat(found).hasSize(1);
        return found.get(0);
    }

    private Team teamByCfbd(long cfbdTeamId) {
        Long externalId = Long.valueOf(cfbdTeamId);
        for (Team team : teamRepository.findAll()) {
            if (externalId.equals(team.getCfbdTeamId())) {
                return team;
            }
        }
        throw new AssertionError("missing team " + cfbdTeamId);
    }

    private Venue venueByCfbd(long cfbdVenueId) {
        Long externalId = Long.valueOf(cfbdVenueId);
        for (Venue venue : venueRepository.findAll()) {
            if (externalId.equals(venue.getCfbdVenueId())) {
                return venue;
            }
        }
        throw new AssertionError("missing venue " + cfbdVenueId);
    }

    private List<Team> teamsNamed(String name) {
        List<Team> found = new ArrayList<>();
        for (Team team : teamRepository.findAll()) {
            if (name.equals(team.getTeamName())) {
                found.add(team);
            }
        }
        return found;
    }

    private static LocalDate firstMondayInAugust(int year) {
        LocalDate cursor = LocalDate.of(year, 8, 1);
        while (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private static int uniqueYear() {
        return 7000 + ThreadLocalRandom.current().nextInt(1500);
    }
}
