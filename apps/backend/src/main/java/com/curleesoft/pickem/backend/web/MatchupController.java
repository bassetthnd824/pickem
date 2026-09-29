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

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.service.MatchupService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for matchups. Replaces {@code MatchupAction}.
 * {@code winningTeamId} is derived from the scores. A client that needs the
 * home team's stadium calls {@code GET /api/manager/teams/{id}} and copies
 * {@code homeVenue}; this resource stores the submitted {@code venueId}.
 */
@RestController
@RequestMapping("/api/manager/matchups")
@PreAuthorize("hasRole('MANAGER')")
public class MatchupController {

    private final MatchupService matchupService;

    public MatchupController(MatchupService matchupService) {
        this.matchupService = matchupService;
    }

    @GetMapping
    public List<Matchup> list(@RequestParam(required = false) String seasonId,
            @RequestParam(required = false) String seasonWeekId, @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) String matchupDate, @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String venueId, @RequestParam(required = false) Long cfbdGameId) {
        return matchupService.search(seasonId, seasonWeekId, weekNumber, matchupDate, teamId, venueId, cfbdGameId);
    }

    @GetMapping("/{id}")
    public Matchup get(@PathVariable String id) {
        return matchupService.get(id);
    }

    @PostMapping
    public ResponseEntity<Matchup> create(@Valid @RequestBody Matchup request) {
        Matchup saved = matchupService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Matchup update(@PathVariable String id, @Valid @RequestBody Matchup request) {
        return matchupService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        matchupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
