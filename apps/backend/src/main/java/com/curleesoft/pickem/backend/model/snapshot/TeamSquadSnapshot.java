package com.curleesoft.pickem.backend.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denormalized team fields embedded on matchups.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamSquadSnapshot {

    private String id;
    private String name;
    private String squad;
}
