package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code picks} document. One row per user per matchup.
 */
@Getter
@Setter
public class Pick extends AuditableDocument {

    @NotBlank
    private String userId;

    @NotBlank
    private String matchupId;

    @NotBlank
    private String seasonId;

    @NotBlank
    private String seasonWeekId;

    @NotBlank
    private String pickedTeamId;

    @NotNull
    private Integer rank;
}
