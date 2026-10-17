package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code venues} document.
 */
@Getter
@Setter
public class Venue extends AuditableDocument {

    @NotBlank
    @Size(max = 60)
    private String venueName;

    @NotBlank
    @Size(max = 60)
    private String cityState;

    private Long cfbdVenueId;
}
