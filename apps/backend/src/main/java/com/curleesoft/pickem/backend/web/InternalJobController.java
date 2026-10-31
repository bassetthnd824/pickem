package com.curleesoft.pickem.backend.web;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curleesoft.pickem.backend.service.CfbdImportService;

/**
 * Scheduler and local entry for internal jobs. The caller is authenticated by
 * {@code InternalJobFilter}. A matchup import with a {@code jobId} runs that
 * job here. Score sync is still a later story. The browser session cookie is
 * not a credential for these paths.
 */
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {

    private final CfbdImportService cfbdImportService;

    public InternalJobController(CfbdImportService cfbdImportService) {
        this.cfbdImportService = cfbdImportService;
    }

    @PostMapping("/cfbd-scores")
    public ResponseEntity<InternalJobAccepted> scores() {
        return accepted("cfbd-scores");
    }

    /**
     * Without a job id this only acknowledges the worker, so a probe does not
     * call College Football Data. With a job id it runs that import to completion.
     */
    @PostMapping("/cfbd-matchups")
    public ResponseEntity<InternalJobAccepted> matchups(@RequestBody(required = false) ImportWorkerRequest request) {
        if (request != null && StringUtils.hasText(request.jobId())) {
            String jobId = Objects.requireNonNull(request.jobId(), "jobId").trim();
            cfbdImportService.execute(jobId);
        }
        return accepted("cfbd-matchups");
    }

    private static ResponseEntity<InternalJobAccepted> accepted(String job) {
        String name = Objects.requireNonNull(job, "job");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new InternalJobAccepted(name));
    }
}
