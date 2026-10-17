package com.curleesoft.pickem.backend.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denormalized team fields embedded on rivalries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamNameSnapshot {

    private String id;
    private String name;
}
