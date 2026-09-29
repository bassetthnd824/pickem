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

import com.curleesoft.pickem.backend.model.Theme;
import com.curleesoft.pickem.backend.service.ThemeService;

import jakarta.validation.Valid;

/**
 * Manager CRUD for themes. Replaces {@code ThemeAction}. {@code themePath} is
 * the theme key and must not begin with {@code /}.
 */
@RestController
@RequestMapping("/api/manager/themes")
@PreAuthorize("hasRole('MANAGER')")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public List<Theme> list(@RequestParam(required = false) String themeName,
            @RequestParam(required = false) String themePath, @RequestParam(required = false) Boolean active) {
        return themeService.search(themeName, themePath, active);
    }

    @GetMapping("/{id}")
    public Theme get(@PathVariable String id) {
        return themeService.get(id);
    }

    @PostMapping
    public ResponseEntity<Theme> create(@Valid @RequestBody Theme request) {
        Theme saved = themeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Theme update(@PathVariable String id, @Valid @RequestBody Theme request) {
        return themeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        themeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
