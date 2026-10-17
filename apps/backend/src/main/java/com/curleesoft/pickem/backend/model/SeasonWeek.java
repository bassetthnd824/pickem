package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code seasonWeeks} document. Calendar dates are ISO-8601 {@code YYYY-MM-DD}.
 */
@Getter
@Setter
public class SeasonWeek extends AuditableDocument {

    @NotBlank
    private String seasonId;

    @NotNull
    private Integer weekNumber;

    @NotBlank
    private String beginDate;

    @NotBlank
    private String endDate;
}
