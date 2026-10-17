package com.curleesoft.pickem.backend.model.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denormalized team fields embedded on rivalries.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeamNameSnapshot {

    private String id;
    private String name;

    public TeamNameSnapshot(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
