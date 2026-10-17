package com.curleesoft.pickem.backend.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denormalized venue fields embedded on teams and matchups.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VenueSnapshot {

    private String id;
    private String name;
    private String cityState;
}
