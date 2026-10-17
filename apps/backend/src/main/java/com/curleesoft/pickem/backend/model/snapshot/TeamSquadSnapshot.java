package com.curleesoft.pickem.backend.model.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denormalized team fields embedded on matchups.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeamSquadSnapshot {

    private String id;
    private String name;
    private String squad;

    public TeamSquadSnapshot(String id, String name, String squad) {
        this.id = id;
        this.name = name;
        this.squad = squad;
    }
}
