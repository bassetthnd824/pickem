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
        String seasonId = season.getId();
        Map<String, Pick> picks = picksByMatchup(userId, seasonId);
        List<Matchup> matchups = matchupRepository.query(collection -> collection.whereEqualTo("seasonId", seasonId));
        List<GameMain.GameWeek> weeks = seasonWeekRepository
                .query(collection -> collection.whereEqualTo("seasonId", seasonId)).stream()
                .sorted(Comparator.comparing((SeasonWeek week) -> week.getWeekNumber(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(week -> toWeek(week, matchups, picks)).toList();
        return new GameMain(season.getId(), season.getSeason(), conferenceTeamCount(), weeks);
    }

    /**
     * Upserts the caller's picks for one week and deletes a row whose team was
     * cleared. Rows that omit the matchup are left as they are. Ranks are 1..N,
     * where N is the number of conference teams, and unique for this user and week.
     * The same plan is checked again inside the commit, against the reread week.
     */
    public List<Pick> saveWeek(String userId, String seasonWeekId, List<PickSelection> selections) {
        if (!StringUtils.hasText(seasonWeekId)) {
            throw new InvalidRequestException(WEEK_REQUIRED);
        }

        SeasonWeek week = seasonWeekRepository.findById(seasonWeekId.trim())
                .orElseThrow(() -> new InvalidRequestException(SeasonWeekService.NOT_FOUND));
        int maxRank = conferenceTeamCount();
        Map<String, Matchup> matchups = matchupsById(week.getId());
        WeekPlan plan = plan(userId, week, maxRank, matchups, picksForWeek(userId, week), selections);
        return pickRepository.commitWeek(userId, week.getId(), plan.upserts(), plan.deletes(),
                stored -> plan(userId, week, maxRank, matchups, stored, selections));
    }

    private WeekPlan plan(String userId, SeasonWeek week, int maxRank, Map<String, Matchup> matchups,
            Map<String, Pick> existing, List<PickSelection> selections) {
        Map<String, Pick> remaining = new HashMap<>(existing);
        List<PickSelection> rows = selections == null ? List.of() : selections;
        Set<String> seen = new HashSet<>();
        List<Pick> upserts = new ArrayList<>();
        List<Pick> deletes = new ArrayList<>();

        for (PickSelection row : rows) {
            if (row == null) {
                continue;
            }

            String matchupId = SearchText.trim(row.matchupId());
            String pickedTeamId = SearchText.trim(row.pickedTeamId());

            if (!StringUtils.hasText(pickedTeamId)) {
                clear(matchupId, remaining, seen, deletes);
                continue;
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

            Pick pick = remaining.remove(matchupId);

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

        Set<Integer> ranks = new HashSet<>();

        for (Pick pick : upserts) {
            if (!ranks.add(pick.getRank())) {
                throw new InvalidRequestException(RANK_NOT_UNIQUE);
            }
        }

        for (Pick kept : remaining.values()) {
            if (kept.getRank() != null && !ranks.add(kept.getRank())) {
                throw new InvalidRequestException(RANK_NOT_UNIQUE);
            }
        }

        return new WeekPlan(upserts, deletes);
    }

    private void clear(String matchupId, Map<String, Pick> remaining, Set<String> seen, List<Pick> deletes) {
        if (!StringUtils.hasText(matchupId)) {
            return;
        }

        if (!seen.add(matchupId)) {
            throw new InvalidRequestException(MATCHUP_DUPLICATE);
        }

        Pick prior = remaining.remove(matchupId);

        if (prior != null && StringUtils.hasText(prior.getId())) {
            deletes.add(prior);
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

        for (Matchup matchup : matchupRepository
                .query(collection -> collection.whereEqualTo("seasonWeekId", seasonWeekId))) {
            if (StringUtils.hasText(matchup.getId())) {
                matchups.put(matchup.getId(), matchup);
            }
        }

        return matchups;
    }

    private Map<String, Pick> picksForWeek(String userId, SeasonWeek week) {
        Map<String, Pick> picks = new HashMap<>();

        for (Pick pick : picksByMatchup(userId, week.getSeasonId()).values()) {
            if (week.getId().equals(pick.getSeasonWeekId())) {
                picks.put(pick.getMatchupId(), pick);
            }
        }

        return picks;
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
                .thenComparing((Matchup matchup) -> matchup.getMatchupDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
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

    private record WeekPlan(List<Pick> upserts, List<Pick> deletes) {
    }
}