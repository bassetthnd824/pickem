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

import com.curleesoft.pickem.backend.model.AuditableDocument;
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
 * Future weeks add 0 even if scores are already stored. A second pick for the
 * same user and matchup does not add again. Refusing a save once the week has
 * begun is US-34.
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
        Map<String, Matchup> matchups = byId(
                matchupRepository.query(collection -> collection.whereEqualTo("seasonId", id)));
        Map<String, SeasonWeek> weeks = byId(
                seasonWeekRepository.query(collection -> collection.whereEqualTo("seasonId", id)));
        Map<String, Long> totals = scorePicks(id, matchups, weeks);
        List<Scored> scored = new ArrayList<>();

        for (User user : userRepository.findAll()) {
            String uid = user.getUid();

            if (!StringUtils.hasText(uid)) {
                continue;
            }

            Long total = totals.get(uid);
            scored.add(new Scored(uid, user.getNickName(), total == null ? 0L : total.longValue()));
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
        String resolved = SearchText.blankToNull(seasonId);

        if (resolved == null) {
            return seasonService.current();
        }

        return seasonService.get(resolved);
    }

    private static <T extends AuditableDocument> Map<String, T> byId(List<T> rows) {
        Map<String, T> indexed = new HashMap<>();

        for (T row : rows) {
            if (StringUtils.hasText(row.getId())) {
                indexed.put(row.getId(), row);
            }
        }

        return indexed;
    }

    private Map<String, Long> scorePicks(String seasonId, Map<String, Matchup> matchups, Map<String, SeasonWeek> weeks) {
        Map<String, Long> totals = new HashMap<>();
        Map<String, Set<String>> counted = new HashMap<>();

        for (Pick pick : pickRepository.query(collection -> collection.whereEqualTo("seasonId", seasonId))) {
            if (!StringUtils.hasText(pick.getUserId()) || !StringUtils.hasText(pick.getMatchupId())) {
                continue;
            }

            Set<String> matchupIds = counted.get(pick.getUserId());

            if (matchupIds == null) {
                matchupIds = new HashSet<>();
                counted.put(pick.getUserId(), matchupIds);
            }

            if (!matchupIds.add(pick.getMatchupId())) {
                continue;
            }

            Long current = totals.get(pick.getUserId());
            long soFar = current == null ? 0L : current.longValue();
            totals.put(pick.getUserId(), Long.valueOf(soFar + points(pick, matchups, weeks)));
        }

        return totals;
    }

    private int points(Pick pick, Map<String, Matchup> matchups, Map<String, SeasonWeek> weeks) {
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
        return Comparator.comparingLong((Scored row) -> row.score()).reversed()
                .thenComparing((Scored left, Scored right) -> left.uid().compareTo(right.uid()));
    }

    private record Scored(String uid, String nickName, long score) {
    }
}
