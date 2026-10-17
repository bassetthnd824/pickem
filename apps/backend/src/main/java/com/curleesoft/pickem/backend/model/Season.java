package com.curleesoft.pickem.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.firestore.annotation.PropertyName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code seasons} document. Calendar dates are ISO-8601 {@code YYYY-MM-DD}.
 */
@Getter
@Setter
public class Season extends AuditableDocument {

    @NotBlank
    private String season;

    @NotBlank
    private String beginDate;

    @NotBlank
    private String endDate;

    @NotNull
    @Getter(onMethod_ = { @JsonProperty("isCurrent"), @PropertyName("isCurrent") })
    @Setter(onMethod_ = { @JsonProperty("isCurrent"), @PropertyName("isCurrent") })
    private boolean current;
}
