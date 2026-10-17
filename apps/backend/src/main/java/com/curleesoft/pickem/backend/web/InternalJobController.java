package com.curleesoft.pickem.backend.web;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Scheduler and local entry for internal jobs. The caller is authenticated by
 * {@code InternalJobFilter}. This controller does not import games or scores.
 */
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {

    @PostMapping("/cfbd-scores")
    public ResponseEntity<InternalJobAccepted> scores() {
        return accepted("cfbd-scores");
    }

    @PostMapping("/cfbd-matchups")
    public ResponseEntity<InternalJobAccepted> matchups() {
        return accepted("cfbd-matchups");
    }

    private static ResponseEntity<InternalJobAccepted> accepted(String job) {
        String name = Objects.requireNonNull(job, "job");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new InternalJobAccepted(name));
    }
}
