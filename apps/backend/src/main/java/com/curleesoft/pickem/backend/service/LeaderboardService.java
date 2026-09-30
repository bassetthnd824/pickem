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
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;

/**
 * Season leaderboard. Ports {@code LeaderBoardAction},
 * {@code MatchupBean.getLeaderBoardForSeason}, and {@code UserScore.compareTo}.
 * Matchups and picks are joined in memory. A correct pick adds its rank only
 * when the week has begun, both scores are set, and the pick is the winner.
 * Future weeks add 0 even if scores are already stored. Refusing a save once
 * the week has begun is US-34.
 */
@Service
public class LeaderboardService {

    private final UserRepository userRepository;

    private final PickRepository pickRepository;

    private final MatchupRepository matchupRepository;

    private final SeasonWeekRepository seasonWeekRepository;

    private final SeasonService seasonService;

    private final Clock clock;

    public LeaderboardService(UserRepository userRepository, PickRepository pickRepository,
            MatchupRepository matchupRepository, SeasonWeekRepository seasonWeekRepository, SeasonService seasonService,
            Clock clock) {
        this.userRepository = userRepository;
        this.pickRepository = pickRepository;
        this.matchupRepository = matchupRepository;
        this.seasonWeekRepository = seasonWeekRepository;
        this.seasonService = seasonService;
        this.clock = clock;
    }

    public Leaderboard leaderboard(String seasonId) {
        Season season = season(seasonId);
        String id = season.getId();

        if (!StringUtils.hasText(id)) {
            throw new ResourceNotFoundException(SeasonService.NOT_FOUND);
        }

        Map<String, Matchup> matchups = matchups(id);
        Map<String, SeasonWeek> weeks = weeks(id);
        Map<String, List<Pick>> picks = picksByUser(id);
        List<Scored> scored = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (User user : userRepository.findAll()) {
            if (user == null || !StringUtils.hasText(user.getUid()) || !seen.add(user.getUid())) {
                continue;
            }

            scored.add(new Scored(user.getUid(), user.getNickName(), score(picks.get(user.getUid()), matchups, weeks)));
        }

        scored.sort(order());
        List<Leaderboard.Standing> standings = new ArrayList<>(scored.size());

        for (int place = 0; place < scored.size(); place++) {
            Scored row = scored.get(place);
            standings.add(new Leaderboard.Standing(place + 1, row.uid(), row.nickName(), row.score()));
        }

        return new Leaderboard(id, season.getSeason(), List.copyOf(standings));
    }

    private Season season(String seasonId) {
        String id = SearchText.blankToNull(seasonId);

        if (id == null) {
            return seasonService.current();
        }

        return seasonService.get(id);
    }

    private Map<String, Matchup> matchups(String seasonId) {
        Map<String, Matchup> matchups = new HashMap<>();

        for (Matchup matchup : matchupRepository.query(collection -> collection.whereEqualTo("seasonId", seasonId))) {
            if (matchup != null && StringUtils.hasText(matchup.getId())) {
                matchups.put(matchup.getId(), matchup);
            }
        }

        return matchups;
    }

    private Map<String, SeasonWeek> weeks(String seasonId) {
        Map<String, SeasonWeek> weeks = new HashMap<>();

        for (SeasonWeek week : seasonWeekRepository
                .query(collection -> collection.whereEqualTo("seasonId", seasonId))) {
            if (week != null && StringUtils.hasText(week.getId())) {
                weeks.put(week.getId(), week);
            }
        }

        return weeks;
    }

    private Map<String, List<Pick>> picksByUser(String seasonId) {
        Map<String, List<Pick>> picks = new HashMap<>();

        for (Pick pick : pickRepository.query(collection -> collection.whereEqualTo("seasonId", seasonId))) {
            if (pick == null || !StringUtils.hasText(pick.getUserId())) {
                continue;
            }

            List<Pick> rows = picks.get(pick.getUserId());

            if (rows == null) {
                rows = new ArrayList<>();
                picks.put(pick.getUserId(), rows);
            }

            rows.add(pick);
        }

        return picks;
    }

    private long score(List<Pick> picks, Map<String, Matchup> matchups, Map<String, SeasonWeek> weeks) {
        long total = 0;

        if (picks == null) {
            return total;
        }

        for (Pick pick : picks) {
            total += points(pick, matchups, weeks);
        }

        return total;
    }

    private int points(Pick pick, Map<String, Matchup> matchups, Map<String, SeasonWeek> weeks) {
        if (pick == null || !StringUtils.hasText(pick.getMatchupId())) {
            return 0;
        }

        Matchup matchup = matchups.get(pick.getMatchupId());

        if (matchup == null) {
            return 0;
        }

        SeasonWeek week = weeks.get(matchup.getSeasonWeekId());
        String begin = week == null ? null : week.getBeginDate();

        if (!PickScoring.currentOrPast(begin, clock)) {
            return 0;
        }

        return PickScoring.points(matchup.getHomeTeamScore(), matchup.getAwayTeamScore(), matchup.getWinningTeamId(),
                pick.getPickedTeamId(), pick.getRank());
    }

    private static Comparator<Scored> order() {
        return Comparator.comparingLong((Scored row) -> row.score()).reversed().thenComparing((Scored row) -> row.uid(),
                Comparator.nullsLast((String left, String right) -> left.compareTo(right)));
    }

    private record Scored(String uid, String nickName, long score) {
    }
}
