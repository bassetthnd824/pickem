package com.curleesoft.pickem.backend.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;

/**
 * Weekly confidence picks. Ports {@code MainAction},
 * {@code MatchupBean.getMatchupUserPicksByUserSeason}, and
 * {@code UserPickBean.persistList}. Matchups and picks are joined in memory.
 * The authenticated user owns every written document. Refusing a save once the
 * week has begun is US-34.
 */
@Service
public class PickService {

    public static final String WEEK_REQUIRED = "season week is required";

    public static final String MATCHUP_REQUIRED = "matchup is required";

    public static final String MATCHUP_WEEK = "matchup does not belong to the season week";

    public static final String MATCHUP_DUPLICATE = "matchup is duplicated";

    public static final String TEAM_NOT_IN_MATCHUP = "picked team is not in the matchup";

    public static final String RANK_INVALID = "rank is invalid";

    public static final String RANK_NOT_UNIQUE = "ranks must be unique";

    private final PickRepository pickRepository;

    private final MatchupRepository matchupRepository;

    private final SeasonWeekRepository seasonWeekRepository;

    private final TeamRepository teamRepository;

    private final SeasonService seasonService;

    private final Clock clock;

    public PickService(PickRepository pickRepository, MatchupRepository matchupRepository,
            SeasonWeekRepository seasonWeekRepository, TeamRepository teamRepository, SeasonService seasonService,
            Clock clock) {
        this.pickRepository = pickRepository;
        this.matchupRepository = matchupRepository;
        this.seasonWeekRepository = seasonWeekRepository;
        this.teamRepository = teamRepository;
        this.seasonService = seasonService;
        this.clock = clock;
    }

    public GameMain main(String userId) {
        Season season = seasonService.current();
        Map<String, Pick> picks = picksByMatchup(userId, season.getId());
        List<Matchup> matchups = matchupRepository.findAll().stream()
                .filter(item -> season.getId().equals(item.getSeasonId())).toList();
        List<GameMain.GameWeek> weeks = seasonWeekRepository.findAll().stream()
                .filter(item -> season.getId().equals(item.getSeasonId()))
                .sorted(Comparator.comparing(SeasonWeek::getWeekNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(week -> toWeek(week, matchups, picks)).toList();
        return new GameMain(season.getId(), season.getSeason(), conferenceTeamCount(), weeks);
    }

    /**
     * Upserts the caller's picks for one week and deletes a row whose team was
     * cleared. Rows that omit the matchup are left as they are. Ranks are 1..N,
     * where N is the number of conference teams, and unique for this user and week.
     */
    public List<Pick> saveWeek(String userId, String seasonWeekId, List<PickSelection> selections) {
        if (!StringUtils.hasText(seasonWeekId)) {
            throw new InvalidRequestException(WEEK_REQUIRED);
        }

        SeasonWeek week = seasonWeekRepository.findById(seasonWeekId.trim())
                .orElseThrow(() -> new InvalidRequestException(SeasonWeekService.NOT_FOUND));
        int maxRank = conferenceTeamCount();
        Map<String, Matchup> matchups = matchupsById(week.getId());
        Map<String, Pick> existing = new HashMap<>();

        for (Pick pick : picksByMatchup(userId, week.getSeasonId()).values()) {
            if (week.getId().equals(pick.getSeasonWeekId())) {
                existing.put(pick.getMatchupId(), pick);
            }
        }
        List<PickSelection> rows = selections == null ? List.of() : selections;
        Set<String> seen = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        List<Pick> upserts = new ArrayList<>();
        List<String> deletes = new ArrayList<>();

        for (PickSelection row : rows) {
            applyRow(userId, week, maxRank, matchups, existing, seen, ranks, upserts, deletes, row);
        }

        for (Pick kept : existing.values()) {
            if (kept.getRank() != null && !ranks.add(kept.getRank())) {
                throw new InvalidRequestException(RANK_NOT_UNIQUE);
            }
        }

        return pickRepository.commitWeek(upserts, deletes);
    }

    private void applyRow(String userId, SeasonWeek week, int maxRank, Map<String, Matchup> matchups,
            Map<String, Pick> existing, Set<String> seen, Set<Integer> ranks, List<Pick> upserts, List<String> deletes,
            PickSelection row) {
        String matchupId = trim(row == null ? null : row.matchupId());
        String pickedTeamId = trim(row == null ? null : row.pickedTeamId());

        if (!StringUtils.hasText(pickedTeamId)) {
            drop(matchupId, existing, seen, deletes);
            return;
        }

        if (!StringUtils.hasText(matchupId)) {
            throw new InvalidRequestException(MATCHUP_REQUIRED);
        }

        if (!seen.add(matchupId)) {
            throw new InvalidRequestException(MATCHUP_DUPLICATE);
        }

        Matchup matchup = matchup(matchups, matchupId);

        if (!pickedTeamId.equals(matchup.getHomeTeamId()) && !pickedTeamId.equals(matchup.getAwayTeamId())) {
            throw new InvalidRequestException(TEAM_NOT_IN_MATCHUP);
        }

        Integer rank = row.rank();

        if (rank == null || rank.intValue() < 1 || rank.intValue() > maxRank) {
            throw new InvalidRequestException(RANK_INVALID);
        }

        if (!ranks.add(rank)) {
            throw new InvalidRequestException(RANK_NOT_UNIQUE);
        }

        Pick pick = existing.remove(matchupId);

        if (pick == null) {
            pick = new Pick();
            pick.setId(PickRepository.documentId(userId, matchup.getId()));
        }

        pick.setUserId(userId);
        pick.setMatchupId(matchup.getId());
        pick.setSeasonId(week.getSeasonId());
        pick.setSeasonWeekId(week.getId());
        pick.setPickedTeamId(pickedTeamId);
        pick.setRank(rank);
        upserts.add(pick);
    }

    private void drop(String matchupId, Map<String, Pick> existing, Set<String> seen, List<String> deletes) {
        if (!StringUtils.hasText(matchupId)) {
            return;
        }

        if (!seen.add(matchupId)) {
            throw new InvalidRequestException(MATCHUP_DUPLICATE);
        }

        Pick prior = existing.remove(matchupId);

        if (prior != null && StringUtils.hasText(prior.getId())) {
            deletes.add(prior.getId());
        }
    }

    private Matchup matchup(Map<String, Matchup> matchups, String matchupId) {
        Matchup matchup = matchups.get(matchupId);

        if (matchup != null) {
            return matchup;
        }

        if (matchupRepository.findById(matchupId).isEmpty()) {
            throw new InvalidRequestException(MatchupService.NOT_FOUND);
        }

        throw new InvalidRequestException(MATCHUP_WEEK);
    }

    private Map<String, Matchup> matchupsById(String seasonWeekId) {
        Map<String, Matchup> matchups = new HashMap<>();

        for (Matchup matchup : matchupRepository.findAll()) {
            if (seasonWeekId.equals(matchup.getSeasonWeekId()) && StringUtils.hasText(matchup.getId())) {
                matchups.put(matchup.getId(), matchup);
            }
        }

        return matchups;
    }

    private Map<String, Pick> picksByMatchup(String userId, String seasonId) {
        Map<String, Pick> picks = new HashMap<>();

        for (Pick pick : pickRepository.findByUserId(userId)) {
            if (seasonId.equals(pick.getSeasonId()) && StringUtils.hasText(pick.getMatchupId())) {
                picks.putIfAbsent(pick.getMatchupId(), pick);
            }
        }

        return picks;
    }

    private int conferenceTeamCount() {
        int count = 0;

        for (Team team : teamRepository.findAll()) {
            if (Boolean.TRUE.equals(team.getConferenceMember())) {
                count++;
            }
        }

        return count;
    }

    private GameMain.GameWeek toWeek(SeasonWeek week, List<Matchup> matchups, Map<String, Pick> picks) {
        boolean open = PickScoring.currentOrPast(week.getBeginDate(), clock);
        List<GameMain.GameMatchup> games = matchups.stream()
                .filter(item -> week.getId().equals(item.getSeasonWeekId())).sorted(gridOrder(picks))
                .map(item -> toMatchup(item, picks.get(item.getId()), open)).toList();
        return new GameMain.GameWeek(week.getId(), week.getWeekNumber(), week.getBeginDate(), week.getEndDate(), open,
                games);
    }

    private static GameMain.GameMatchup toMatchup(Matchup matchup, Pick pick, boolean open) {
        String pickedTeamId = pick == null ? null : pick.getPickedTeamId();
        Integer rank = pick == null ? null : pick.getRank();
        Integer points = open ? Integer.valueOf(PickScoring.points(matchup.getHomeTeamScore(), matchup.getAwayTeamScore(),
                matchup.getWinningTeamId(), pickedTeamId, rank)) : null;
        return new GameMain.GameMatchup(matchup.getId(), matchup.getMatchupDate(), matchup.getAwayTeam(),
                open ? matchup.getAwayTeamScore() : null, matchup.getHomeTeam(), open ? matchup.getHomeTeamScore() : null,
                matchup.getVenue(), matchup.getRivalryName(), pick == null ? null : pick.getId(), pickedTeamId, rank,
                open ? matchup.getWinningTeamId() : null, points);
    }

    private static Comparator<Matchup> gridOrder(Map<String, Pick> picks) {
        return Comparator
                .comparing((Matchup matchup) -> rankOf(picks.get(matchup.getId())),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Matchup::getMatchupDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PickService::homeName, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static Integer rankOf(Pick pick) {
        return pick == null ? null : pick.getRank();
    }

    private static String homeName(Matchup matchup) {
        if (matchup.getHomeTeam() == null) {
            return null;
        }

        return matchup.getHomeTeam().getName();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}