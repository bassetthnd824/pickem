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

import com.curleesoft.pickem.backend.model.Rivalry;
import com.curleesoft.pickem.backend.service.RivalryService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for rivalries. Replaces {@code RivalryAction}. The two teams
 * must differ; snapshots are filled from the team documents.
 */
@RestController
@RequestMapping("/api/manager/rivalries")
@PreAuthorize("hasRole('MANAGER')")
public class RivalryController {

    private final RivalryService rivalryService;

    public RivalryController(RivalryService rivalryService) {
        this.rivalryService = rivalryService;
    }

    @GetMapping
    public List<Rivalry> list(@RequestParam(required = false) String rivalryName,
            @RequestParam(required = false) String team1Id, @RequestParam(required = false) String team2Id) {
        return rivalryService.search(rivalryName, team1Id, team2Id);
    }

    @GetMapping("/{id}")
    public Rivalry get(@PathVariable String id) {
        return rivalryService.get(id);
    }

    @PostMapping
    public ResponseEntity<Rivalry> create(@Valid @RequestBody Rivalry request) {
        Rivalry saved = rivalryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Rivalry update(@PathVariable String id, @Valid @RequestBody Rivalry request) {
        return rivalryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        rivalryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
