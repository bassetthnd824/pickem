package com.curleesoft.pickem.backend.model;

import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code teams} document. Embeds a home-venue snapshot and keeps
 * {@code homeVenueId} for edits. {@code homeVenue} is written by the service
 * from the venue document and may be omitted on input.
 */
@Getter
@Setter
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
}
