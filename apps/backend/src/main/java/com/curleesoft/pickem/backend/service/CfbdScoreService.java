package com.curleesoft.pickem.backend.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.cfbd.CfbdClientException;
import com.curleesoft.pickem.backend.cfbd.CfbdGame;
import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;

/**
 * Game-day score sync. Loads the current season's matchups dated today in
 * {@code America/New_York}, plus yesterday's matchups that still have no
 * scores (a kickoff that spilled past midnight). For each one with a
 * {@code cfbdGameId}, a completed CFBD game that has both point totals and a
 * winner replaces {@code homeTeamScore} / {@code awayTeamScore} and
 * {@code winningTeamId}. The same totals are not written again. Uncompleted
 * games, missing totals, and ties are left as stored. A day with nothing to
 * sync does not call College Football Data.
 */
@Slf4j
@Service
public class CfbdScoreService {

    private static final int MAX_ATTEMPTS = 3;

    private final SeasonRepository seasonRepository;

    private final MatchupRepository matchupRepository;

    private final CfbdClient cfbdClient;

    private final CfbdPause cfbdPause;

    private final Clock clock;

    public CfbdScoreService(SeasonRepository seasonRepository, MatchupRepository matchupRepository,
            CfbdClient cfbdClient, CfbdPause cfbdPause, Clock clock) {
        this.seasonRepository = seasonRepository;
        this.matchupRepository = matchupRepository;
        this.cfbdClient = cfbdClient;
        this.cfbdPause = cfbdPause;
        this.clock = clock;
    }

    /**
     * Runs one sync. CFBD failures are logged and rethrown so the HTTP entry
     * returns 503 and Cloud Scheduler retries. Matchups that were not written
     * stay as they were.
     */
    public ScoreSyncResult syncToday() {
        Clock leagueClock = Objects.requireNonNull(clock, "clock");
        LocalDate today;
        try {
            today = LocalDate.now(leagueClock.withZone(PickScoring.LEAGUE_ZONE));
        } catch (DateTimeException ex) {
            throw new IllegalStateException("score sync clock is invalid", ex);
        }
        Optional<Season> current = SeasonCalendar.selectCurrent(seasonRepository.findAll(), today);
        if (current.isEmpty()) {
            log.info("CFBD score sync found no current season on {}", today);
            return new ScoreSyncResult(0, 0);
        }
        Season season = current.get();
        String seasonId = Objects.requireNonNull(season.getId(), "seasonId");
        int year = seasonYear(season.getSeason());
        List<Matchup> due = dueMatchups(seasonId, today);
        if (due.isEmpty()) {
            log.info("CFBD score sync has no matchups for {} in season {}", today, year);
            return new ScoreSyncResult(0, 0);
        }
        Map<Long, CfbdGame> games;
        try {
            games = loadGames(year, due);
        } catch (CfbdClientException ex) {
            log.warn("CFBD score sync failed: {}", ex.getMessage());
            throw ex;
        }
        int updated = 0;
        for (Matchup found : due) {
            Matchup matchup = Objects.requireNonNull(found, "matchup");
            if (apply(matchup, gameFor(games, matchup.getCfbdGameId()))) {
                updated++;
            }
        }
        log.info("CFBD score sync updated {} of {} matchups", updated, due.size());
        return new ScoreSyncResult(due.size(), updated);
    }

    private List<Matchup> dueMatchups(String seasonId, LocalDate today) {
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        LocalDate leagueToday = Objects.requireNonNull(today, "today");
        LocalDate yesterday = leagueToday.minusDays(1);
        List<Matchup> due = new ArrayList<>();
        for (Matchup found : matchupsFor(parentId)) {
            Matchup matchup = Objects.requireNonNull(found, "matchup");
            if (isDue(matchup, leagueToday, yesterday)) {
                due.add(matchup);
            }
        }
        return due;
    }

    private List<Matchup> matchupsFor(String seasonId) {
        String parentId = Objects.requireNonNull(seasonId, "seasonId");
        return matchupRepository.query(collection -> {
            CollectionReference matchups = Objects.requireNonNull(collection, "collection");
            Query query = Objects.requireNonNull(matchups.whereEqualTo("seasonId", parentId), "query");
            return query;
        });
    }

    /**
     * Today, or yesterday when both scores are still empty. Older games and
     * rows without a CFBD id are left alone.
     */
    private static boolean isDue(Matchup matchup, LocalDate today, LocalDate yesterday) {
        if (matchup.getCfbdGameId() == null) {
            return false;
        }
        LocalDate played = matchupDate(matchup);
        if (played == null) {
            return false;
        }
        if (played.equals(today)) {
            return true;
        }
        return played.equals(yesterday) && !bothScores(matchup);
    }

    private static LocalDate matchupDate(Matchup matchup) {
        String value = matchup.getMatchupDate();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            log.warn("CFBD score sync skipped matchup {} with date {}", matchup.getId(), value);
            return null;
        }
    }

    private static boolean bothScores(Matchup matchup) {
        return matchup.getHomeTeamScore() != null && matchup.getAwayTeamScore() != null;
    }

    private Map<Long, CfbdGame> loadGames(int year, List<Matchup> due) {
        boolean regular = false;
        boolean postseason = false;
        for (Matchup matchup : due) {
            Integer weekNumber = matchup.getWeekNumber();
            if (weekNumber != null && weekNumber.intValue() > CfbdImportService.POSTSEASON_WEEK_BASE) {
                postseason = true;
            } else {
                regular = true;
            }
        }
        Map<Long, CfbdGame> games = new LinkedHashMap<>();
        if (regular) {
            addGames(games, callCfbd(() -> cfbdClient.games(year, null, CfbdImportService.REGULAR)));
        }
        if (postseason) {
            addGames(games, callCfbd(() -> cfbdClient.games(year, null, CfbdImportService.POSTSEASON)));
        }
        return games;
    }

    private static void addGames(Map<Long, CfbdGame> games, List<CfbdGame> listed) {
        Map<Long, CfbdGame> target = Objects.requireNonNull(games, "games");
        if (listed == null) {
            throw new CfbdClientException(CfbdClientException.EMPTY);
        }
        for (CfbdGame found : listed) {
            if (found != null) {
                target.put(Long.valueOf(found.id()), found);
            }
        }
    }

    private static CfbdGame gameFor(Map<Long, CfbdGame> games, Long cfbdGameId) {
        if (cfbdGameId == null) {
            return null;
        }
        Long gameId = Objects.requireNonNull(cfbdGameId, "cfbdGameId");
        return games.get(gameId);
    }

    /**
     * Home wins when {@code homePoints > awayPoints}; otherwise away. That is
     * {@code MatchupUserPick.getWinningTeamId()} once both totals exist and
     * are not equal. A tie is not stored.
     */
    private boolean apply(Matchup matchup, CfbdGame game) {
        if (game == null) {
            log.warn("CFBD score sync has no game {}", matchup.getCfbdGameId());
            return false;
        }
        Integer homePoints = game.homePoints();
        Integer awayPoints = game.awayPoints();
        if (!game.completed() || homePoints == null || awayPoints == null) {
            return false;
        }
        if (homePoints.equals(awayPoints)) {
            log.warn("CFBD score sync skipped tied game {}", Long.valueOf(game.id()));
            return false;
        }
        return writeIfChanged(matchup, homePoints, awayPoints);
    }

    private boolean writeIfChanged(Matchup matchup, Integer homePoints, Integer awayPoints) {
        Integer home = Objects.requireNonNull(homePoints, "homePoints");
        Integer away = Objects.requireNonNull(awayPoints, "awayPoints");
        String homeId = Objects.requireNonNull(matchup.getHomeTeamId(), "homeTeamId");
        String awayId = Objects.requireNonNull(matchup.getAwayTeamId(), "awayTeamId");
        String winner = home.compareTo(away) > 0 ? homeId : awayId;
        if (home.equals(matchup.getHomeTeamScore()) && away.equals(matchup.getAwayTeamScore())
                && winner.equals(matchup.getWinningTeamId())) {
            return false;
        }
        matchup.setHomeTeamScore(home);
        matchup.setAwayTeamScore(away);
        matchup.setWinningTeamId(winner);
        matchupRepository.save(matchup);
        return true;
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
                log.warn("CFBD score sync retry {} after status {}", tryNumber, ex.upstreamStatus());
                pause.pause(tryNumber);
            }
        }
        throw Objects.requireNonNull(last, "cfbd");
    }

    private static boolean isTransient(CfbdClientException ex) {
        int status = ex.upstreamStatus();
        return status == 429 || status >= 500;
    }

    private static int seasonYear(String season) {
        if (season == null || !season.matches("\\d{4}")) {
            throw new IllegalStateException("season year is invalid");
        }
        return Integer.parseInt(season);
    }

    /**
     * @param considered matchups the job tried to score
     * @param updated matchups whose stored scores or winner changed
     */
    public record ScoreSyncResult(int considered, int updated) {
    }
}
