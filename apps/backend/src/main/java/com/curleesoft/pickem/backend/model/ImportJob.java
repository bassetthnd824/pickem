package com.curleesoft.pickem.backend.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Operational status for one CFBD schedule import. Not one of the nine domain
 * collections. The browser polls this document; it is not client-readable in
 * Firestore rules.
 */
@Getter
@Setter
public class ImportJob extends AuditableDocument {

    public static final String QUEUED = "queued";

    public static final String RUNNING = "running";

    public static final String SUCCEEDED = "succeeded";

    public static final String FAILED = "failed";

    private String seasonId;

    private Integer seasonYear;

    private String seasonType;

    private String status;

    private String error;

    private Integer weeksCreated;

    private Integer weeksUpdated;

    private Integer teamsCreated;

    private Integer venuesCreated;

    private Integer matchupsCreated;

    private Integer matchupsUpdated;

    private List<ImportJobSkip> skipped = new ArrayList<>();
}
