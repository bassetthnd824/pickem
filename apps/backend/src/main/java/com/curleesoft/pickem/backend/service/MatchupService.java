package com.curleesoft.pickem.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Rivalry;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for matchups. Ports {@code MatchupAction}. Snapshots, the week
 * number, and the rivalry name are copied onto this document at save time.
 * Refreshing them after a team or venue edit is US-35. The venue is whatever
 * the client submits; {@code GET /api/manager/teams/{id}} already returns
 * {@code homeVenue} so the client can default it when the home team changes.
 * <p>
 * {@code winningTeamId} is derived here. Unplayed games (both scores empty)
 * have no winner. A strict home lead selects the home team; otherwise the away
 * team. Equal scores are rejected: college football has no ties.
 */
@Service
public class MatchupService {

    public static final String NOT_FOUND = "Matchup not found";

    public static final String DATE_INVALID = "matchup date is invalid";

    public static final String TEAMS_REQUIRED = "matchup teams are required";

    public static final String TEAMS_EQUAL = "teams cannot be equal";

    public static final String SEASON_MISMATCH = "season week does not belong to the season";

    public static final String WEEK_NUMBER_MISMATCH = "week number does not match the season week";

    public static final String SCORES_INCOMPLETE = "scores must both be set or both be empty";

    public static final String SCORES_TIED = "tied scores are invalid";

    public static final String CFBD_NOT_UNIQUE = "cfbdGameId must be unique";

    private final MatchupRepository matchupRepository;

    private final SeasonRepository seasonRepository;

    private final SeasonWeekRepository seasonWeekRepository;

    private final TeamRepository teamRepository;

    private final VenueRepository venueRepository;

    private final RivalryRepository rivalryRepository;

    public MatchupService(MatchupRepository matchupRepository, SeasonRepository seasonRepository,
            SeasonWeekRepository seasonWeekRepository, TeamRepository teamRepository, VenueRepository venueRepository,
            RivalryRepository rivalryRepository) {
        this.matchupRepository = matchupRepository;
        this.seasonRepository = seasonRepository;
        this.seasonWeekRepository = seasonWeekRepository;
        this.teamRepository = teamRepository;
        this.venueRepository = venueRepository;
        this.rivalryRepository = rivalryRepository;
    }

    /**
     * Search-by-example. {@code teamId} matches either side, which is how the
     * legacy matchup search form looks up one team. Order follows
     * {@code MatchupBean}: season year, week number, date, home team name.
     */
    public List<Matchup> search(String seasonId, String seasonWeekId, Integer weekNumber, String matchupDate,
            String teamId, String venueId, Long cfbdGameId) {
        String season = blankToNull(seasonId);
        String weekId = blankToNull(seasonWeekId);
        String date = blankToNull(matchupDate);
        String team = blankToNull(teamId);
        String venue = blankToNull(venueId);
        Map<String, String> seasonYearById = seasonYears();

        return matchupRepository.findAll().stream()
                .filter(item -> season == null || season.equals(item.getSeasonId()))
                .filter(item -> weekId == null || weekId.equals(item.getSeasonWeekId()))
                .filter(item -> weekNumber == null || weekNumber.equals(item.getWeekNumber()))
                .filter(item -> date == null || date.equals(item.getMatchupDate()))
                .filter(item -> team == null || team.equals(item.getHomeTeamId()) || team.equals(item.getAwayTeamId()))
                .filter(item -> venue == null || venue.equals(item.getVenueId()))
                .filter(item -> cfbdGameId == null || cfbdGameId.equals(item.getCfbdGameId()))
                .sorted(matchupOrder(seasonYearById)).toList();
    }

    public Matchup get(String id) {
        return matchupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Matchup create(Matchup request) {
        Matchup matchup = new Matchup();
        apply(matchup, request);
        return matchupRepository.save(matchup, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, matchup));
    }

    public Matchup update(String id, Matchup request) {
        Matchup existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return matchupRepository.save(existing, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, existing));
    }

    public void delete(String id) {
        get(id);
        matchupRepository.delete(id);
    }

    private void apply(Matchup target, Matchup request) {
        String seasonWeekId = trim(request.getSeasonWeekId());
        SeasonWeek week = seasonWeekRepository.findById(seasonWeekId)
                .orElseThrow(() -> new InvalidRequestException(SeasonWeekService.NOT_FOUND));

        String seasonId = trim(request.getSeasonId());

        if (!StringUtils.hasText(seasonId) || !seasonId.equals(week.getSeasonId())) {
            throw new InvalidRequestException(SEASON_MISMATCH);
        }

        if (request.getWeekNumber() != null && !request.getWeekNumber().equals(week.getWeekNumber())) {
            throw new InvalidRequestException(WEEK_NUMBER_MISMATCH);
        }

        LocalDate date = parseDate(trim(request.getMatchupDate()));
        LocalDate begin = parseDate(week.getBeginDate());
        LocalDate end = parseDate(week.getEndDate());

        if (date.isBefore(begin) || date.isAfter(end)) {
            throw new InvalidRequestException(DATE_INVALID);
        }

        String homeId = trim(request.getHomeTeamId());
        String awayId = trim(request.getAwayTeamId());

        if (!StringUtils.hasText(homeId) || !StringUtils.hasText(awayId)) {
            throw new InvalidRequestException(TEAMS_REQUIRED);
        }

        if (homeId.equals(awayId)) {
            throw new InvalidRequestException(TEAMS_EQUAL);
        }

        Team home = teamRepository.findById(homeId)
                .orElseThrow(() -> new InvalidRequestException(TeamService.NOT_FOUND));
        Team away = teamRepository.findById(awayId)
                .orElseThrow(() -> new InvalidRequestException(TeamService.NOT_FOUND));
        String venueId = trim(request.getVenueId());
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new InvalidRequestException(VenueService.NOT_FOUND));
        Integer homeScore = request.getHomeTeamScore();
        Integer awayScore = request.getAwayTeamScore();

        target.setSeasonId(week.getSeasonId());
        target.setSeasonWeekId(week.getId());
        target.setWeekNumber(week.getWeekNumber());
        target.setMatchupDate(date.toString());
        target.setHomeTeamId(home.getId());
        target.setAwayTeamId(away.getId());
        target.setHomeTeam(new TeamSquadSnapshot(home.getId(), home.getTeamName(), home.getSquadName()));
        target.setAwayTeam(new TeamSquadSnapshot(away.getId(), away.getTeamName(), away.getSquadName()));
        target.setHomeTeamScore(homeScore);
        target.setAwayTeamScore(awayScore);
        target.setWinningTeamId(winner(homeScore, awayScore, home.getId(), away.getId()));
        target.setVenueId(venue.getId());
        target.setVenue(new VenueSnapshot(venue.getId(), venue.getVenueName(), venue.getCityState()));
        target.setRivalryName(rivalryName(home.getId(), away.getId()));
        target.setCfbdGameId(request.getCfbdGameId());
    }

    private static String winner(Integer homeScore, Integer awayScore, String homeId, String awayId) {
        if (homeScore == null && awayScore == null) {
            return null;
        }

        if (homeScore == null || awayScore == null) {
            throw new InvalidRequestException(SCORES_INCOMPLETE);
        }

        if (homeScore.equals(awayScore)) {
            throw new InvalidRequestException(SCORES_TIED);
        }

        return homeScore.compareTo(awayScore) > 0 ? homeId : awayId;
    }

    /**
     * The pair may be stored in either order. Several rivalries for the same
     * pair keep the alphabetically first name so the snapshot is stable.
     */
    private String rivalryName(String homeId, String awayId) {
        String chosen = null;

        for (Rivalry rivalry : rivalryRepository.findAll()) {
            if (!pairs(rivalry, homeId, awayId) || !StringUtils.hasText(rivalry.getRivalryName())) {
                continue;
            }

            if (chosen == null || rivalry.getRivalryName().compareTo(chosen) < 0) {
                chosen = rivalry.getRivalryName();
            }
        }

        return chosen;
    }

    private static boolean pairs(Rivalry rivalry, String homeId, String awayId) {
        String first = rivalry.getTeam1Id();
        String second = rivalry.getTeam2Id();
        return (homeId.equals(first) && awayId.equals(second)) || (homeId.equals(second) && awayId.equals(first));
    }

    private static void rejectDuplicate(Transaction transaction, CollectionReference collection, String documentId,
            Matchup matchup) {
        Long externalId = matchup.getCfbdGameId();

        if (externalId == null) {
            return;
        }

        if (TransactionReads.anotherDocumentMatches(transaction, collection.whereEqualTo("cfbdGameId", externalId),
                documentId)) {
            throw new ConflictException(CFBD_NOT_UNIQUE);
        }
    }

    private Map<String, String> seasonYears() {
        Map<String, String> seasonYearById = new HashMap<>();

        for (Season season : seasonRepository.findAll()) {
            if (season.getId() != null) {
                seasonYearById.put(season.getId(), season.getSeason());
            }
        }

        return seasonYearById;
    }

    private static Comparator<Matchup> matchupOrder(Map<String, String> seasonYearById) {
        return Comparator
                .comparing((Matchup item) -> seasonYearById.get(item.getSeasonId()),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right)))
                .thenComparing((Matchup item) -> item.getWeekNumber(),
                        Comparator.nullsLast((Integer left, Integer right) -> left.compareTo(right)))
                .thenComparing((Matchup item) -> item.getMatchupDate(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right)))
                .thenComparing((Matchup item) -> item.getHomeTeam() == null ? null : item.getHomeTeam().getName(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right)));
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidRequestException(DATE_INVALID);
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException(DATE_INVALID);
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
