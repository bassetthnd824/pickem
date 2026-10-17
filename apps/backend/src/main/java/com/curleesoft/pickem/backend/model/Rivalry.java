package com.curleesoft.pickem.backend.model;

import com.curleesoft.pickem.backend.model.snapshot.TeamNameSnapshot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code rivalries} document. Embeds both team snapshots. {@code team1} and
 * {@code team2} are written by the service from the team documents and may be
 * omitted on input. {@code team1Id} and {@code team2Id} must differ.
 */
@Getter
@Setter
public class Rivalry extends AuditableDocument {

    @NotBlank
    @Size(max = 60)
    private String rivalryName;

    @NotBlank
    private String team1Id;

    @NotBlank
    private String team2Id;

    private TeamNameSnapshot team1;

    private TeamNameSnapshot team2;
}
