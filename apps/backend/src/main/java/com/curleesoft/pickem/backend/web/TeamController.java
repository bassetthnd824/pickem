package com.curleesoft.pickem.backend.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.service.TeamService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for teams. {@code GET /{id}} includes the home-venue snapshot
 * (replaces {@code teams_getTeamById}). {@code conferenceMember=true} replaces
 * {@code TeamBean.getConferenceTeams}.
 */
@RestController
@RequestMapping("/api/manager/teams")
@PreAuthorize("hasRole('MANAGER')")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<Team> list(@RequestParam(required = false) String teamName,
            @RequestParam(required = false) String squadName,
            @RequestParam(required = false) Boolean conferenceMember,
            @RequestParam(required = false) String homeVenueId, @RequestParam(required = false) Long cfbdTeamId) {
        return teamService.search(teamName, squadName, conferenceMember, homeVenueId, cfbdTeamId);
    }

    @GetMapping("/{id}")
    public Team get(@PathVariable String id) {
        return teamService.get(id);
    }

    @PostMapping
    public ResponseEntity<Team> create(@Valid @RequestBody Team request) {
        Team saved = teamService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Team update(@PathVariable String id, @Valid @RequestBody Team request) {
        return teamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
