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

import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.service.VenueService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for venues. Replaces {@code VenueAction}.
 */
@RestController
@RequestMapping("/api/manager/venues")
@PreAuthorize("hasRole('MANAGER')")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public List<Venue> list(@RequestParam(required = false) String venueName,
            @RequestParam(required = false) String cityState, @RequestParam(required = false) Long cfbdVenueId) {
        return venueService.search(venueName, cityState, cfbdVenueId);
    }

    @GetMapping("/{id}")
    public Venue get(@PathVariable String id) {
        return venueService.get(id);
    }

    @PostMapping
    public ResponseEntity<Venue> create(@Valid @RequestBody Venue request) {
        Venue saved = venueService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Venue update(@PathVariable String id, @Valid @RequestBody Venue request) {
        return venueService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        venueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
