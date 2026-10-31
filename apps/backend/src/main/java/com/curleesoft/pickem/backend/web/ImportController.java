package com.curleesoft.pickem.backend.web;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curleesoft.pickem.backend.model.ImportJob;
import com.curleesoft.pickem.backend.service.CfbdImportService;
import com.curleesoft.pickem.backend.service.ImportJobDispatcher;

import jakarta.validation.Valid;

/**
 * Manager entry for a CFBD schedule import. The browser reaches these paths
 * through the Next.js {@code /api/manager/**} allowlist. The worker stays on
 * {@code /api/internal/jobs/cfbd-matchups} and is not part of that allowlist.
 */
@RestController
@RequestMapping("/api/manager/imports")
@PreAuthorize("hasRole('MANAGER')")
public class ImportController {

    private final CfbdImportService cfbdImportService;

    private final ImportJobDispatcher importJobDispatcher;

    public ImportController(CfbdImportService cfbdImportService, ImportJobDispatcher importJobDispatcher) {
        this.cfbdImportService = cfbdImportService;
        this.importJobDispatcher = importJobDispatcher;
    }

    @PostMapping("/matchups")
    public ResponseEntity<ImportAccepted> enqueue(@Valid @RequestBody ImportMatchupsRequest request) {
        ImportMatchupsRequest body = Objects.requireNonNull(request, "request");
        ImportJob job = cfbdImportService.enqueue(body.seasonId(), body.seasonType());
        String jobId = Objects.requireNonNull(job.getId(), "jobId");
        importJobDispatcher.dispatch(jobId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ImportAccepted(jobId));
    }

    @GetMapping("/{jobId}")
    public ImportJob get(@PathVariable String jobId) {
        return cfbdImportService.get(jobId);
    }
}
