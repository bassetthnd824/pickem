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

import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.service.SeasonWeekService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for season weeks. Replaces {@code SeasonWeekAction},
 * {@code seasonWeeks_getSeasonWeeksBySeason}, and {@code getSeasonWeekById}.
 */
@RestController
@RequestMapping("/api/manager/season-weeks")
@PreAuthorize("hasRole('MANAGER')")
public class SeasonWeekController {

    private final SeasonWeekService seasonWeekService;

    public SeasonWeekController(SeasonWeekService seasonWeekService) {
        this.seasonWeekService = seasonWeekService;
    }

    @GetMapping
    public List<SeasonWeek> list(@RequestParam(required = false) String seasonId,
            @RequestParam(required = false) Integer weekNumber, @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate) {
        return seasonWeekService.search(seasonId, weekNumber, beginDate, endDate);
    }

    @GetMapping("/{id}")
    public SeasonWeek get(@PathVariable String id) {
        return seasonWeekService.get(id);
    }

    @PostMapping
    public ResponseEntity<SeasonWeek> create(@Valid @RequestBody SeasonWeek request) {
        SeasonWeek saved = seasonWeekService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public SeasonWeek update(@PathVariable String id, @Valid @RequestBody SeasonWeek request) {
        return seasonWeekService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        seasonWeekService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
