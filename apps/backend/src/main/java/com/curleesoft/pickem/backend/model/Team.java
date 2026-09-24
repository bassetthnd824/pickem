package com.curleesoft.pickem.backend.model;

import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code teams} document. Embeds a home-venue snapshot and keeps
 * {@code homeVenueId} for edits. {@code homeVenue} is written by the service
 * from the venue document and may be omitted on input.
 */
public class Team extends AuditableDocument {

    @NotBlank
    @Size(max = 40)
    private String teamName;

    @NotBlank
    @Size(max = 40)
    private String squadName;

    @NotNull
    private Boolean conferenceMember;

    @NotBlank
    private String homeVenueId;

    private VenueSnapshot homeVenue;

    private Long cfbdTeamId;

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getSquadName() {
        return squadName;
    }

    public void setSquadName(String squadName) {
        this.squadName = squadName;
    }

    public Boolean getConferenceMember() {
        return conferenceMember;
    }

    public void setConferenceMember(Boolean conferenceMember) {
        this.conferenceMember = conferenceMember;
    }

    public String getHomeVenueId() {
        return homeVenueId;
    }

    public void setHomeVenueId(String homeVenueId) {
        this.homeVenueId = homeVenueId;
    }

    public VenueSnapshot getHomeVenue() {
        return homeVenue;
    }

    public void setHomeVenue(VenueSnapshot homeVenue) {
        this.homeVenue = homeVenue;
    }

    public Long getCfbdTeamId() {
        return cfbdTeamId;
    }

    public void setCfbdTeamId(Long cfbdTeamId) {
        this.cfbdTeamId = cfbdTeamId;
    }
}
