package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;

/**
 * Conference-team schedule. Ports {@code TeamScheduleAction} and
 * {@code MatchupBean.getMatchupsBySeasonTeam}. The opponent label is the other
 * team's name, prefixed with {@code "at "} when the selected team is away.
 * W/L is from that team's perspective. The score string stays home score, then
 * away score. Unplayed games (either score empty, or equal scores) omit both.
 */
@Service
public class TeamScheduleService {

    public static final String TEAM_REQUIRED = "teamId is required";

    public static final String NOT_CONFERENCE = "team is not a conference member";

    static final String AWAY_PREFIX = "at ";

    private final MatchupRepository matchupRepository;

    private final TeamRepository teamRepository;

    private final SeasonService seasonService;

    public TeamScheduleService(MatchupRepository matchupRepository, TeamRepository teamRepository,
            SeasonService seasonService) {
        this.matchupRepository = matchupRepository;
        this.teamRepository = teamRepository;
        this.seasonService = seasonService;
    }

    public List<TeamScheduleRow> schedule(String teamId, String seasonId) {
        String team = SearchText.blankToNull(teamId);

        if (team == null) {
            throw new InvalidRequestException(TEAM_REQUIRED);
        }

        Team selected = teamRepository.findById(team)
                .orElseThrow(() -> new ResourceNotFoundException(TeamService.NOT_FOUND));

        if (!Boolean.TRUE.equals(selected.getConferenceMember())) {
            throw new InvalidRequestException(NOT_CONFERENCE);
        }

        Season season = seasonService.resolve(seasonId);
        String id = season.getId();

        return matchupRepository.query(collection -> collection.whereEqualTo("seasonId", id)).stream()
                .filter(item -> involves(item, team)).sorted(scheduleOrder())
                .map(item -> row(item, team)).toList();
    }

    private TeamScheduleRow row(Matchup matchup, String teamId) {
        boolean home = teamId.equals(matchup.getHomeTeamId());
        String opponent = home ? name(matchup.getAwayTeam()) : AWAY_PREFIX + name(matchup.getHomeTeam());
        Integer homeScore = matchup.getHomeTeamScore();
        Integer awayScore = matchup.getAwayTeamScore();

        if (homeScore == null || awayScore == null || homeScore.equals(awayScore)) {
            return new TeamScheduleRow(matchup.getMatchupDate(), opponent, null, null);
        }

        String scoreResult = homeScore + " - " + awayScore;
        String winLoss;

        if (home) {
            winLoss = homeScore.intValue() > awayScore.intValue() ? "W" : "L";
        } else {
            winLoss = homeScore.intValue() > awayScore.intValue() ? "L" : "W";
        }

        return new TeamScheduleRow(matchup.getMatchupDate(), opponent, scoreResult, winLoss);
    }

    private static String name(TeamSquadSnapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getName())) {
            return "";
        }

        return snapshot.getName();
    }

    private static boolean involves(Matchup matchup, String teamId) {
        return teamId.equals(matchup.getHomeTeamId()) || teamId.equals(matchup.getAwayTeamId());
    }

    private static Comparator<Matchup> scheduleOrder() {
        return Comparator
                .comparing((Matchup item) -> item.getWeekNumber(),
                        Comparator.nullsLast((Integer left, Integer right) -> left.compareTo(right)))
                .thenComparing((Matchup item) -> item.getMatchupDate(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right)))
                .thenComparing((Matchup item) -> homeName(item),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right)));
    }

    private static String homeName(Matchup matchup) {
        TeamSquadSnapshot home = matchup.getHomeTeam();

        if (home == null) {
            return null;
        }

        return home.getName();
    }
}
