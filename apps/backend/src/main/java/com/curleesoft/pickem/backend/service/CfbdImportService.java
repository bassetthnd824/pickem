package com.curleesoft.pickem.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.cfbd.CfbdCalendarWeek;
import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.cfbd.CfbdClientException;
import com.curleesoft.pickem.backend.cfbd.CfbdGame;
import com.curleesoft.pickem.backend.cfbd.CfbdVenue;
import com.curleesoft.pickem.backend.model.ImportJob;
import com.curleesoft.pickem.backend.model.ImportJobSkip;
import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.repository.ImportJobRepository;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.StaleDocumentVersionException;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.ImportActorAuthentication;
import com.curleesoft.pickem.backend.seed.SeedCatalog;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;

/**
 * Enqueues a season import and, when the worker runs, upserts CFBD calendar
 * weeks, missing opponents, venues, and matchups. Week dates are the CFBD
 * calendar dates. They are not forced onto Thursday–Wednesday. Manual week
 * CRUD stays on {@link SeasonWeekService}.
 * <p>
 * One CFBD game id becomes one matchup. A re-run updates that row's date,
 * venue, and teams. Conference-team ids that are not already stored are
 * skipped. Stored scores stay with the same teams. A completed game with both
 * point totals and a winner replaces them. The import does not write picks.
 */
@Slf4j
@Service
public class CfbdImportService {

    public static final String NOT_FOUND = "Import job not found";

    public static final String SEASON_YEAR_INVALID = "season year is invalid";

    public static final String SEASON_TYPE_INVALID = "seasonType is invalid";

    static final String REGULAR = "regular";

    static final String POSTSEASON = "postseason";

    /**
     * CFBD numbers postseason weeks from 1 again. Stored week numbers stay unique
     * per season, so postseason week 1 is 101 and does not replace regular week 1.
     */
    static final int POSTSEASON_WEEK_BASE = 100;

    static final String MISSING_CONFERENCE_TEAM = "missing conference team";

    static final String MISSING_TEAM_ID = "missing team id";

    static final String NEUTRAL_VENUE = "neutral site has no CFBD venue";

    private static final int MAX_ATTEMPTS = 3;

    private static final int NAME_LIMIT = 40;

    private static final int VENUE_LIMIT = 60;

    private static final String ACTOR = ImportActorAuthentication.ACTOR;

    private final ImportJobRepository importJobRepository;

    private final SeasonRepository seasonRepository;

    private final SeasonWeekRepository seasonWeekRepository;

    private final TeamRepository teamRepository;

    private final VenueRepository venueRepository;

    private final MatchupRepository matchupRepository;

    private final TeamService teamService;

    private final VenueService venueService;

    private final MatchupService matchupService;

    private final CfbdClient cfbdClient;

    private final CfbdPause cfbdPause;

    private final ConcurrentHashMap<String, Object> seasonLocks = new ConcurrentHashMap<>();

    public CfbdImportService(ImportJobRepository importJobRepository, SeasonRepository seasonRepository,
            SeasonWeekRepository seasonWeekRepository, TeamRepository teamRepository, VenueRepository venueRepository,
            MatchupRepository matchupRepository, TeamService teamService, VenueService venueService,
            MatchupService matchupService, CfbdClient cfbdClient, CfbdPause cfbdPause) {
        this.importJobRepository = importJobRepository;
        this.seasonRepository = seasonRepository;
        this.seasonWeekRepository = seasonWeekRepository;
        this.teamRepository = teamRepository;
        this.venueRepository = venueRepository;
        this.matchupRepository = matchupRepository;
        this.teamService = teamService;
        this.venueService = venueService;
        this.matchupService = matchupService;
        this.cfbdClient = cfbdClient;
        this.cfbdPause = cfbdPause;
    }

    /**
     * Records a queued job. The caller schedules {@link #execute(String)} so this
     * method does not call College Football Data.
     */
    public ImportJob enqueue(String seasonId, String seasonType) {
        String id = SearchText.blankToNull(seasonId);
        if (id == null) {
            throw new InvalidRequestException(SEASON_YEAR_INVALID);
        }
        Season season = seasonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(SeasonService.NOT_FOUND));
        int year = seasonYear(season.getSeason());
        ImportJob job = new ImportJob();
        job.setSeasonId(season.getId());
        job.setSeasonYear(year);
        job.setSeasonType(normalizeSeasonType(seasonType));
        job.setStatus(ImportJob.QUEUED);
        resetCounts(job);
        return importJobRepository.save(job, ACTOR);
    }

    public ImportJob get(String jobId) {
        String id = SearchText.blankToNull(jobId);
        if (id == null) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }
        return importJobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    /**
     * Runs one job to completion. Transient CFBD 429 and 5xx responses are retried.
     * The last failure is stored on the job and rethrown.
     */
    public void execute(String jobId) {
        ImportJob claimed = get(jobId);
        String seasonId = Objects.requireNonNull(claimed.getSeasonId(), "seasonId");
        Object lock = seasonLocks.computeIfAbsent(seasonId, id -> new Object());
        synchronized (lock) {
            runLocked(jobId);
        }
    }

    /**
     * Marks a job failed when the worker never starts. The import itself records
     * its own failure inside {@link #execute(String)}.
     */
    public void recordStartFailure(String jobId, String message) {
        try {
            ImportJob job = get(jobId);
            job.setStatus(ImportJob.FAILED);
            job.setError(message(new IllegalStateException(message)));
            importJobRepository.save(job, ACTOR);
        } catch (RuntimeException ex) {
            log.warn("CFBD import {} could not record a start failure: {}", jobId, ex.getMessage());
        }
    }

    private void runLocked(String jobId) {
        ImportJob job = get(jobId);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new ImportActorAuthentication());
        SecurityContextHolder.setContext(context);
        try {
            job.setStatus(ImportJob.RUNNING);
            job.setError(null);
            resetCounts(job);
            job = importJobRepository.save(job, ACTOR);
            doImport(job);
            job.setStatus(ImportJob.SUCCEEDED);
            job.setError(null);
            importJobRepository.save(job, ACTOR);
        } catch (RuntimeException ex) {
            fail(job, ex);
            throw ex;
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private void doImport(ImportJob job) {
        Integer yearValue = Objects.requireNonNull(job.getSeasonYear(), "seasonYear");
        int year = yearValue.intValue();
        String seasonType = Objects.requireNonNull(job.getSeasonType(), "seasonType");
        String seasonId = Objects.requireNonNull(job.getSeasonId(), "seasonId");
        CfbdClient client = Objects.requireNonNull(cfbdClient, "cfbdClient");

        List<CfbdCalendarWeek> calendar = callCfbd(() -> client.calendar(year));
        if (calendar == null) {
            throw new CfbdClientException(CfbdClientException.EMPTY);
        }
        Map<Integer, SeasonWeek> weeks = upsertWeeks(seasonId, year, seasonType, calendar, job);

        List<CfbdGame> listed = callCfbd(() -> client.games(year, null, seasonType));
        if (listed == null) {
            throw new CfbdClientException(CfbdClientException.EMPTY);
        }
        Map<Long, Team> teams = teamsByExternalId();
        Map<Long, Venue> venues = venuesByExternalId();
        Map<Long, String> places = venuePlaces(listed, venues);
        Venue unknown = ensureUnknownVenue();
        for (CfbdGame game : deduped(listed).values()) {
            importGame(job, seasonId, seasonType, year, weeks, teams, venues, places, unknown, game);
        }
    }

    private Map<Integer, SeasonWeek> upsertWeeks(String seasonId, int year, String seasonType,
            List<CfbdCalendarWeek> calendar, ImportJob job) {
        Map<Integer, SeasonWeek> existing = weeksFor(seasonId);
        Map<Integer, SeasonWeek> written = new HashMap<>();
        for (CfbdCalendarWeek found : calendar) {
            if (found == null || found.season() != year || !seasonType.equals(found.seasonType())) {
                continue;
            }
            LocalDate begin;
            LocalDate end;
            try {
                begin = CfbdDates.weekBegin(found.startDate());
                end = CfbdDates.weekEnd(found.endDate());
            } catch (RuntimeException ex) {
                skip(job, null, "calendar week " + found.week() + " dates are invalid");
                continue;
            }
            if (end.isBefore(begin)) {
                skip(job, null, "calendar week " + found.week() + " dates are invalid");
                continue;
            }
            int number = storedWeekNumber(seasonType, found.week());
            SeasonWeek saved = saveWeekResilient(seasonId, number, begin, end, existing.get(Integer.valueOf(number)),
                    job);
            existing.put(Integer.valueOf(number), saved);
            written.put(Integer.valueOf(number), saved);
        }
        return written;
    }

    private SeasonWeek saveWeekResilient(String seasonId, int weekNumber, LocalDate begin, LocalDate end,
            SeasonWeek current, ImportJob job) {
        try {
            return saveWeek(seasonId, weekNumber, begin, end, current, job);
        } catch (StaleDocumentVersionException ex) {
            if (current == null || current.getId() == null) {
                throw ex;
            }
            String id = current.getId();
            SeasonWeek fresh = seasonWeekRepository.findById(id).orElseThrow(() -> ex);
            return saveWeek(seasonId, weekNumber, begin, end, fresh, job);
        }
    }

    private SeasonWeek saveWeek(String seasonId, int weekNumber, LocalDate begin, LocalDate end, SeasonWeek current,
            ImportJob job) {
        boolean created = current == null;
        SeasonWeek week = created ? new SeasonWeek() : current;
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        Integer number = Integer.valueOf(weekNumber);
        week.setSeasonId(parentId);
        week.setWeekNumber(number);
        week.setBeginDate(begin.toString());
        week.setEndDate(end.toString());
        SeasonWeek saved = seasonWeekRepository.save(week, ACTOR, (transaction, collection, documentId) -> rejectDuplicateWeek(
                transaction, collection, documentId, parentId, number));
        if (created) {
            job.setWeeksCreated(count(job.getWeeksCreated()) + 1);
        } else {
            job.setWeeksUpdated(count(job.getWeeksUpdated()) + 1);
        }
        return saved;
    }

    private static void rejectDuplicateWeek(Transaction transaction, CollectionReference collection, String documentId,
            String seasonId, Integer weekNumber) {
        CollectionReference weeks = Objects.requireNonNull(collection, "collection");
        String id = Objects.requireNonNull(documentId, "documentId");
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        Integer number = Objects.requireNonNull(weekNumber, "weekNumber");
        Query query = Objects.requireNonNull(weeks.whereEqualTo("seasonId", parentId), "query");
        QuerySnapshot snapshot = TransactionReads.get(transaction, query);
        for (QueryDocumentSnapshot found : snapshot.getDocuments()) {
            if (found.getId().equals(id)) {
                continue;
            }
            SeasonWeek other = found.toObject(SeasonWeek.class);
            if (other != null && number.equals(other.getWeekNumber())) {
                throw new ConflictException(SeasonWeekService.NOT_UNIQUE);
            }
        }
    }

    private void importGame(ImportJob job, String seasonId, String seasonType, int year, Map<Integer, SeasonWeek> weeks,
            Map<Long, Team> teams, Map<Long, Venue> venues, Map<Long, String> places, Venue unknown, CfbdGame game) {
        CfbdGame row = Objects.requireNonNull(game, "game");
        Long gameId = Long.valueOf(row.id());
        if (row.season() != year || !seasonType.equals(row.seasonType())) {
            skip(job, gameId, "game is outside the requested season");
            return;
        }
        SeasonWeek week = weeks.get(Integer.valueOf(storedWeekNumber(seasonType, row.week())));
        if (week == null || week.getId() == null) {
            skip(job, gameId, "no calendar week");
            return;
        }
        LocalDate date;
        try {
            date = CfbdDates.kickoffDate(row.startDate());
            LocalDate begin = LocalDate.parse(week.getBeginDate());
            LocalDate end = LocalDate.parse(week.getEndDate());
            if (date.isBefore(begin) || date.isAfter(end)) {
                skip(job, gameId, "game date is outside the calendar week");
                return;
            }
        } catch (RuntimeException ex) {
            skip(job, gameId, "game date is invalid");
            return;
        }
        if (missingConferenceTeam(row.homeCfbdTeamId(), row.homeConference(), teams)
                || missingConferenceTeam(row.awayCfbdTeamId(), row.awayConference(), teams)) {
            skip(job, gameId, MISSING_CONFERENCE_TEAM);
            return;
        }
        if (row.homeCfbdTeamId() <= 0 || row.awayCfbdTeamId() <= 0) {
            skip(job, gameId, MISSING_TEAM_ID);
            return;
        }
        if (row.homeCfbdTeamId() == row.awayCfbdTeamId()) {
            skip(job, gameId, MatchupService.TEAMS_EQUAL);
            return;
        }
        if (Boolean.TRUE.equals(row.neutralSite()) && row.cfbdVenueId() == null) {
            skip(job, gameId, NEUTRAL_VENUE);
            return;
        }
        try {
            Team home = resolveTeam(teams, row.homeCfbdTeamId(), row.homeTeam(), true, row, venues, places, unknown,
                    job);
            Team away = resolveTeam(teams, row.awayCfbdTeamId(), row.awayTeam(), false, row, venues, places, unknown,
                    job);
            Venue venue = matchupVenue(row, venues, places, home, job);
            upsertMatchup(job, seasonId, week, date, home, away, venue, row);
        } catch (InvalidRequestException | ConflictException | StaleDocumentVersionException ex) {
            skip(job, gameId, ex.getMessage());
        }
    }

    /**
     * A side that is one of the 16 conference schools, or that CFBD labels SEC,
     * must already exist. Opponents are created later.
     */
    static boolean missingConferenceTeam(long externalId, String conference, Map<Long, Team> teams) {
        Map<Long, Team> known = Objects.requireNonNull(teams, "teams");
        if (externalId > 0 && known.containsKey(Long.valueOf(externalId))) {
            return false;
        }
        return isSec(conference) || (externalId > 0 && SeedCatalog.isConferenceTeamId(externalId));
    }

    private static boolean isSec(String conference) {
        if (conference == null) {
            return false;
        }
        return "SEC".equalsIgnoreCase(conference.trim());
    }

    private Team resolveTeam(Map<Long, Team> teams, long externalId, String cfbdName, boolean homeSide, CfbdGame game,
            Map<Long, Venue> venues, Map<Long, String> places, Venue unknown, ImportJob job) {
        Long id = Long.valueOf(externalId);
        Team existing = teams.get(id);
        if (existing != null) {
            return refreshUnknownHomeVenue(existing, id, homeSide, game, venues, places, job, teams);
        }
        String name = clip(cfbdName, NAME_LIMIT, "Team " + id);
        Team draft = new Team();
        draft.setTeamName(name);
        draft.setSquadName(name);
        draft.setConferenceMember(Boolean.FALSE);
        draft.setCfbdTeamId(id);
        if (homeSide && game.cfbdVenueId() != null && !Boolean.TRUE.equals(game.neutralSite())) {
            draft.setHomeVenueId(resolveVenue(game, venues, places, job).getId());
        } else {
            draft.setHomeVenueId(Objects.requireNonNull(unknown.getId(), "unknownVenue"));
        }
        Team saved;
        try {
            saved = teamService.create(draft);
        } catch (ConflictException ex) {
            if (!TeamService.NAME_NOT_UNIQUE.equals(ex.getMessage())) {
                throw ex;
            }
            draft.setTeamName(uniqueName(name, id));
            saved = teamService.create(draft);
        }
        teams.put(id, saved);
        job.setTeamsCreated(count(job.getTeamsCreated()) + 1);
        return saved;
    }

    private Team refreshUnknownHomeVenue(Team existing, Long externalId, boolean homeSide, CfbdGame game,
            Map<Long, Venue> venues, Map<Long, String> places, ImportJob job, Map<Long, Team> teams) {
        if (!homeSide || game.cfbdVenueId() == null || Boolean.TRUE.equals(game.neutralSite())
                || !SeedCatalog.UNKNOWN_VENUE_ID.equals(existing.getHomeVenueId())) {
            return existing;
        }
        String teamId = Objects.requireNonNull(existing.getId(), "teamId");
        existing.setHomeVenueId(resolveVenue(game, venues, places, job).getId());
        Team updated = teamService.update(teamId, existing);
        teams.put(externalId, updated);
        return updated;
    }

    private Venue matchupVenue(CfbdGame game, Map<Long, Venue> venues, Map<Long, String> places, Team home,
            ImportJob job) {
        if (Boolean.TRUE.equals(game.neutralSite()) || game.cfbdVenueId() != null) {
            return resolveVenue(game, venues, places, job);
        }
        String venueId = Objects.requireNonNull(home.getHomeVenueId(), "homeVenueId");
        return venueRepository.findById(venueId).orElseThrow(() -> new InvalidRequestException(VenueService.NOT_FOUND));
    }

    private Venue resolveVenue(CfbdGame game, Map<Long, Venue> venues, Map<Long, String> places, ImportJob job) {
        Long externalId = Objects.requireNonNull(game.cfbdVenueId(), "cfbdVenueId");
        Venue existing = venues.get(externalId);
        if (existing != null) {
            return refreshUnknownPlace(existing, externalId, places, venues);
        }
        Venue draft = new Venue();
        draft.setVenueName(clip(game.venue(), VENUE_LIMIT, "Venue " + externalId));
        draft.setCityState(placeFor(externalId, places));
        draft.setCfbdVenueId(externalId);
        Venue saved = venueService.create(draft);
        venues.put(externalId, saved);
        job.setVenuesCreated(count(job.getVenuesCreated()) + 1);
        return saved;
    }

    private Venue refreshUnknownPlace(Venue existing, Long externalId, Map<Long, String> places,
            Map<Long, Venue> venues) {
        String place = placeFor(externalId, places);
        String current = existing.getCityState();
        if ("Unknown".equals(place) || Objects.equals(place, current) || !unknownPlace(current)) {
            return existing;
        }
        String venueId = Objects.requireNonNull(existing.getId(), "venueId");
        existing.setCityState(place);
        Venue updated = venueService.update(venueId, existing);
        venues.put(externalId, updated);
        return updated;
    }

    /**
     * Looks up city and state only for venue ids we do not already store with a
     * real city. A catalog failure still leaves those venues importable.
     */
    private Map<Long, String> venuePlaces(List<CfbdGame> games, Map<Long, Venue> venues) {
        List<CfbdGame> rows = Objects.requireNonNull(games, "games");
        Map<Long, Venue> stored = Objects.requireNonNull(venues, "venues");
        boolean needsVenue = false;
        for (CfbdGame found : rows) {
            if (found == null || found.cfbdVenueId() == null) {
                continue;
            }
            Venue known = stored.get(found.cfbdVenueId());
            if (known == null || unknownPlace(known.getCityState())) {
                needsVenue = true;
                break;
            }
        }
        if (!needsVenue) {
            return new HashMap<>();
        }
        CfbdClient client = Objects.requireNonNull(cfbdClient, "cfbdClient");
        List<CfbdVenue> listed;
        try {
            listed = callCfbd(() -> client.venues());
        } catch (CfbdClientException ex) {
            log.warn("CFBD venue catalog unavailable; new venues keep an unknown city and state: {}",
                    ex.getMessage());
            return new HashMap<>();
        }
        if (listed == null) {
            log.warn("CFBD venue catalog was empty; new venues keep an unknown city and state");
            return new HashMap<>();
        }
        Map<Long, String> places = new HashMap<>();
        for (CfbdVenue found : listed) {
            if (found == null) {
                continue;
            }
            places.put(Long.valueOf(found.id()), cityState(found.city(), found.state()));
        }
        return places;
    }

    private static boolean unknownPlace(String cityState) {
        return cityState == null || cityState.isBlank() || "Unknown".equals(cityState);
    }

    private static String placeFor(Long externalId, Map<Long, String> places) {
        Map<Long, String> known = Objects.requireNonNull(places, "places");
        Long id = Objects.requireNonNull(externalId, "cfbdVenueId");
        String found = known.get(id);
        if (found == null || found.isBlank()) {
            return "Unknown";
        }
        return found;
    }

    static String cityState(String city, String state) {
        String town = city == null ? "" : city.trim();
        String region = state == null ? "" : state.trim();
        String combined;
        if (!town.isEmpty() && !region.isEmpty()) {
            combined = town + ", " + region;
        } else if (!town.isEmpty()) {
            combined = town;
        } else if (!region.isEmpty()) {
            combined = region;
        } else {
            combined = "Unknown";
        }
        return clip(combined, VENUE_LIMIT, "Unknown");
    }

    private Venue ensureUnknownVenue() {
        String id = Objects.requireNonNull(SeedCatalog.UNKNOWN_VENUE_ID, "unknownVenueId");
        return venueRepository.findById(id).orElseGet(() -> {
            Venue venue = new Venue();
            venue.setId(id);
            venue.setVenueName("Unknown");
            venue.setCityState("Unknown");
            Venue saved = venueRepository.save(venue, ACTOR);
            return Objects.requireNonNull(saved, "unknownVenue");
        });
    }

    private void upsertMatchup(ImportJob job, String seasonId, SeasonWeek week, LocalDate date, Team home, Team away,
            Venue venue, CfbdGame game) {
        CfbdGame row = Objects.requireNonNull(game, "game");
        Long externalId = Long.valueOf(row.id());
        Matchup existing = matchupByGameId(externalId);
        Matchup request = new Matchup();
        request.setSeasonId(seasonId);
        request.setSeasonWeekId(Objects.requireNonNull(week.getId(), "seasonWeekId"));
        request.setWeekNumber(week.getWeekNumber());
        request.setMatchupDate(date.toString());
        request.setHomeTeamId(Objects.requireNonNull(home.getId(), "homeTeamId"));
        request.setAwayTeamId(Objects.requireNonNull(away.getId(), "awayTeamId"));
        request.setVenueId(Objects.requireNonNull(venue.getId(), "venueId"));
        request.setCfbdGameId(externalId);
        applyScores(request, existing, row);
        if (existing == null) {
            matchupService.create(request);
            job.setMatchupsCreated(count(job.getMatchupsCreated()) + 1);
            return;
        }
        request.setVersion(existing.getVersion());
        matchupService.update(Objects.requireNonNull(existing.getId(), "matchupId"), request);
        job.setMatchupsUpdated(count(job.getMatchupsUpdated()) + 1);
    }

    /**
     * Scores stay with the team that earned them. A completed non-tie replaces
     * them. Otherwise the same pairing keeps the stored totals, a pure swap
     * moves each total with its team, and any other change clears both so a
     * leftover total is not awarded to a new opponent. A tie is left unset
     * because matchup validation would reject the row.
     */
    private static void applyScores(Matchup request, Matchup existing, CfbdGame game) {
        Integer homePoints = game.homePoints();
        Integer awayPoints = game.awayPoints();
        if (game.completed() && homePoints != null && awayPoints != null && !homePoints.equals(awayPoints)) {
            request.setHomeTeamScore(homePoints);
            request.setAwayTeamScore(awayPoints);
            return;
        }
        if (existing != null && Objects.equals(request.getHomeTeamId(), existing.getHomeTeamId())
                && Objects.equals(request.getAwayTeamId(), existing.getAwayTeamId())) {
            request.setHomeTeamScore(existing.getHomeTeamScore());
            request.setAwayTeamScore(existing.getAwayTeamScore());
            return;
        }
        if (existing != null && Objects.equals(request.getHomeTeamId(), existing.getAwayTeamId())
                && Objects.equals(request.getAwayTeamId(), existing.getHomeTeamId())
                && existing.getHomeTeamScore() != null && existing.getAwayTeamScore() != null) {
            request.setHomeTeamScore(existing.getAwayTeamScore());
            request.setAwayTeamScore(existing.getHomeTeamScore());
            return;
        }
        request.setHomeTeamScore(null);
        request.setAwayTeamScore(null);
    }

    private Matchup matchupByGameId(Long gameId) {
        Long externalId = Objects.requireNonNull(gameId, "cfbdGameId");
        List<Matchup> found = matchupRepository.query(collection -> {
            CollectionReference matchups = Objects.requireNonNull(collection, "collection");
            Query query = Objects.requireNonNull(matchups.whereEqualTo("cfbdGameId", externalId), "query");
            return query;
        });
        if (found.isEmpty()) {
            return null;
        }
        return found.get(0);
    }

    private Map<Integer, SeasonWeek> weeksFor(String seasonId) {
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        Map<Integer, SeasonWeek> weeks = new HashMap<>();
        for (SeasonWeek found : seasonWeekRepository.findAll()) {
            SeasonWeek week = Objects.requireNonNull(found, "week");
            if (!parentId.equals(week.getSeasonId()) || week.getWeekNumber() == null) {
                continue;
            }
            weeks.put(week.getWeekNumber(), week);
        }
        return weeks;
    }

    private Map<Long, Team> teamsByExternalId() {
        Map<Long, Team> teams = new HashMap<>();
        for (Team found : teamRepository.findAll()) {
            Team team = Objects.requireNonNull(found, "team");
            Long externalId = team.getCfbdTeamId();
            if (externalId != null) {
                teams.put(externalId, team);
            }
        }
        return teams;
    }

    private Map<Long, Venue> venuesByExternalId() {
        Map<Long, Venue> venues = new HashMap<>();
        for (Venue found : venueRepository.findAll()) {
            Venue venue = Objects.requireNonNull(found, "venue");
            Long externalId = venue.getCfbdVenueId();
            if (externalId != null) {
                venues.put(externalId, venue);
            }
        }
        return venues;
    }

    private static Map<Long, CfbdGame> deduped(List<CfbdGame> listed) {
        Map<Long, CfbdGame> games = new LinkedHashMap<>();
        for (CfbdGame found : listed) {
            if (found == null) {
                continue;
            }
            games.put(Long.valueOf(found.id()), found);
        }
        return games;
    }

    private <T> T callCfbd(Supplier<T> call) {
        Supplier<T> attempt = Objects.requireNonNull(call, "call");
        CfbdPause pause = Objects.requireNonNull(cfbdPause, "cfbdPause");
        CfbdClientException last = null;
        for (int tryNumber = 1; tryNumber <= MAX_ATTEMPTS; tryNumber++) {
            try {
                return attempt.get();
            } catch (CfbdClientException ex) {
                last = ex;
                if (!isTransient(ex) || tryNumber == MAX_ATTEMPTS) {
                    throw ex;
                }
                log.warn("CFBD import retry {} after status {}", tryNumber, ex.upstreamStatus());
                pause.pause(tryNumber);
            }
        }
        throw Objects.requireNonNull(last, "cfbd");
    }

    private static boolean isTransient(CfbdClientException ex) {
        int status = ex.upstreamStatus();
        return status == 429 || status >= 500;
    }

    private void fail(ImportJob job, RuntimeException ex) {
        try {
            job.setStatus(ImportJob.FAILED);
            job.setError(message(ex));
            importJobRepository.save(job, ACTOR);
        } catch (RuntimeException saveEx) {
            log.warn("CFBD import {} could not record failure: {}", job.getId(), saveEx.getMessage());
        }
    }

    private static void resetCounts(ImportJob job) {
        job.setWeeksCreated(0);
        job.setWeeksUpdated(0);
        job.setTeamsCreated(0);
        job.setVenuesCreated(0);
        job.setMatchupsCreated(0);
        job.setMatchupsUpdated(0);
        job.setSkipped(new ArrayList<>());
    }

    private static void skip(ImportJob job, Long gameId, String reason) {
        String text = Objects.requireNonNull(reason, "reason");
        List<ImportJobSkip> skips = job.getSkipped();
        if (skips == null) {
            skips = new ArrayList<>();
            job.setSkipped(skips);
        }
        skips.add(new ImportJobSkip(gameId, text));
    }

    private static int count(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private static int seasonYear(String season) {
        if (season == null || !season.matches("\\d{4}")) {
            throw new InvalidRequestException(SEASON_YEAR_INVALID);
        }
        return Integer.parseInt(season);
    }

    static int storedWeekNumber(String seasonType, int cfbdWeek) {
        if (POSTSEASON.equals(seasonType)) {
            return POSTSEASON_WEEK_BASE + cfbdWeek;
        }
        return cfbdWeek;
    }

    static String normalizeSeasonType(String seasonType) {
        String type = seasonType == null ? "" : seasonType.trim();
        if (type.isEmpty()) {
            return REGULAR;
        }
        if (!REGULAR.equals(type) && !POSTSEASON.equals(type)) {
            throw new InvalidRequestException(SEASON_TYPE_INVALID);
        }
        return type;
    }

    private static String message(RuntimeException ex) {
        String text = ex.getMessage();
        if (text == null || text.isBlank()) {
            return "Import failed";
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private static String clip(String value, int max, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            trimmed = fallback;
        }
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    private static String uniqueName(String name, Long externalId) {
        String suffix = " " + externalId;
        int keep = NAME_LIMIT - suffix.length();
        String base = name == null ? "" : name;
        if (keep < 1) {
            String compact = suffix.trim();
            return compact.length() <= NAME_LIMIT ? compact : compact.substring(0, NAME_LIMIT);
        }
        if (base.length() > keep) {
            base = base.substring(0, keep);
        }
        return base + suffix;
    }
}
