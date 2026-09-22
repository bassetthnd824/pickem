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

import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.service.SeasonService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for seasons. Replaces {@code SeasonAction} and
 * {@code seasons_getSeasonById}.
 */
@RestController
@RequestMapping("/api/manager/seasons")
@PreAuthorize("hasRole('MANAGER')")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    public List<Season> list(@RequestParam(required = false) String season,
            @RequestParam(required = false) String beginDate, @RequestParam(required = false) String endDate,
            @RequestParam(name = "isCurrent", required = false) Boolean current) {
        return seasonService.search(season, beginDate, endDate, current);
    }

    @GetMapping("/current")
    public Season current() {
        return seasonService.current();
    }

    @GetMapping("/{id}")
    public Season get(@PathVariable String id) {
        return seasonService.get(id);
    }

    @PostMapping
    public ResponseEntity<Season> create(@Valid @RequestBody Season request) {
        Season saved = seasonService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Season update(@PathVariable String id, @Valid @RequestBody Season request) {
        return seasonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        seasonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
