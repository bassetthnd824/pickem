package com.curleesoft.pickem.backend.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One game or calendar row the import did not write, with a reason the job
 * status can show.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImportJobSkip {

    private Long cfbdGameId;

    private String reason;

    public ImportJobSkip(Long cfbdGameId, String reason) {
        this.cfbdGameId = cfbdGameId;
        this.reason = reason;
    }
}
