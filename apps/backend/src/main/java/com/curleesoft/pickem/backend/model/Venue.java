package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code venues} document.
 */
public class Venue extends AuditableDocument {

    @NotBlank
    @Size(max = 60)
    private String venueName;

    @NotBlank
    @Size(max = 60)
    private String cityState;

    private Long cfbdVenueId;

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getCityState() {
        return cityState;
    }

    public void setCityState(String cityState) {
        this.cityState = cityState;
    }

    public Long getCfbdVenueId() {
        return cfbdVenueId;
    }

    public void setCfbdVenueId(Long cfbdVenueId) {
        this.cfbdVenueId = cfbdVenueId;
    }
}
