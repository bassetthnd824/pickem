package com.curleesoft.pickem.backend.model.snapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denormalized venue fields embedded on teams and matchups.
 */
@Getter
@Setter
@NoArgsConstructor
public class VenueSnapshot {

    private String id;
    private String name;
    private String cityState;

    public VenueSnapshot(String id, String name, String cityState) {
        this.id = id;
        this.name = name;
        this.cityState = cityState;
    }
}
