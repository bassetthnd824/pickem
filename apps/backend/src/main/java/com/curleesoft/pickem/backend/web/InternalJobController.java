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
import com.curleesoft.pickem.backend.service.CfbdScoreService;
import com.curleesoft.pickem.backend.service.CfbdScoreService.ScoreSyncResult;

/**
 * Scheduler and local entry for internal jobs. The caller is authenticated by
 * {@code InternalJobFilter}. Score sync runs in this request. A matchup import
 * with a {@code jobId} runs that job here. The browser session cookie is not a
 * credential for these paths.
 */
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {

    private final CfbdImportService cfbdImportService;

    private final CfbdScoreService cfbdScoreService;

    public InternalJobController(CfbdImportService cfbdImportService, CfbdScoreService cfbdScoreService) {
        this.cfbdImportService = cfbdImportService;
        this.cfbdScoreService = cfbdScoreService;
    }

    /**
     * Local loopback or {@code X-Pickem-Job-Secret}. See the API contract for
     * the emulator curl and the Cloud Scheduler cron.
     */
    @PostMapping("/cfbd-scores")
    public ResponseEntity<ScoreJobResponse> scores() {
        ScoreSyncResult result = Objects.requireNonNull(cfbdScoreService.syncToday(), "result");
        return ResponseEntity.ok(new ScoreJobResponse("cfbd-scores", result.updated()));
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
