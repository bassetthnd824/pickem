package com.curleesoft.pickem.backend.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.security.InvalidCredentialException;
import com.curleesoft.pickem.backend.security.PickemPrincipal;
import com.curleesoft.pickem.backend.service.AccountProfile;
import com.curleesoft.pickem.backend.service.AccountService;
import com.curleesoft.pickem.backend.service.ConferenceTeam;
import com.curleesoft.pickem.backend.service.GameMain;
import com.curleesoft.pickem.backend.service.Leaderboard;
import com.curleesoft.pickem.backend.service.LeaderboardService;
import com.curleesoft.pickem.backend.service.PickService;
import com.curleesoft.pickem.backend.service.TeamScheduleRow;
import com.curleesoft.pickem.backend.service.TeamScheduleService;
import com.curleesoft.pickem.backend.service.TeamService;

import jakarta.validation.Valid;
import tools.jackson.databind.JsonNode;

/**
 * Player game surface. Replaces {@code MainAction}, {@code LeaderBoardAction},
 * {@code TeamScheduleAction}, and {@code AccountAction}. Any signed-in player
 * or manager may call it. The Next.js BFF is the public caller.
 */
@RestController
@RequestMapping("/api/game")
@PreAuthorize("hasAnyRole('PLAYER', 'MANAGER')")
public class GameController {

    private final PickService pickService;

    private final LeaderboardService leaderboardService;

    private final TeamScheduleService teamScheduleService;

    private final TeamService teamService;

    private final AccountService accountService;

    public GameController(PickService pickService, LeaderboardService leaderboardService,
            TeamScheduleService teamScheduleService, TeamService teamService, AccountService accountService) {
        this.pickService = pickService;
        this.leaderboardService = leaderboardService;
        this.teamScheduleService = teamScheduleService;
        this.teamService = teamService;
        this.accountService = accountService;
    }

    @GetMapping("/main")
    public GameMain main(Authentication authentication) {
        return pickService.main(principal(authentication).uid());
    }

    @GetMapping("/leaderboard")
    public Leaderboard leaderboard(@RequestParam(required = false) String seasonId, Authentication authentication) {
        principal(authentication);
        return leaderboardService.leaderboard(seasonId);
    }

    @GetMapping("/team-schedule")
    public List<TeamScheduleRow> teamSchedule(@RequestParam(required = false) String teamId,
            @RequestParam(required = false) String seasonId, Authentication authentication) {
        principal(authentication);
        return teamScheduleService.schedule(teamId, seasonId);
    }

    @GetMapping("/teams")
    public List<ConferenceTeam> teams(Authentication authentication) {
        principal(authentication);
        return teamService.conferenceTeams().stream()
                .map(team -> new ConferenceTeam(team.getId(), team.getTeamName(), team.getSquadName())).toList();
    }

    @GetMapping("/account")
    public AccountProfile account(Authentication authentication) {
        return accountService.profile(principal(authentication).uid());
    }

    @PutMapping("/account")
    public AccountProfile updateAccount(@RequestBody JsonNode body, Authentication authentication) {
        return accountService.update(principal(authentication).uid(), AccountUpdateBinder.bind(body));
    }

    @PostMapping("/picks")
    public List<Pick> save(@Valid @RequestBody SavePicksRequest request, Authentication authentication) {
        return pickService.saveWeek(principal(authentication).uid(), request.seasonWeekId(), request.picks());
    }

    private static PickemPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PickemPrincipal caller) {
            return caller;
        }

        throw new InvalidCredentialException("Authentication is required");
    }
}