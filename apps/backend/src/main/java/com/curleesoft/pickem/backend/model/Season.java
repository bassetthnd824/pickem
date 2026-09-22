package com.curleesoft.pickem.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.firestore.annotation.PropertyName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code seasons} document. Calendar dates are ISO-8601 {@code YYYY-MM-DD}.
 */
public class Season extends AuditableDocument {

    @NotBlank
    private String season;

    @NotBlank
    private String beginDate;

    @NotBlank
    private String endDate;

    @NotNull
    private boolean current;

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(String beginDate) {
        this.beginDate = beginDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @JsonProperty("isCurrent")
    @PropertyName("isCurrent")
    public boolean isCurrent() {
        return current;
    }

    @JsonProperty("isCurrent")
    @PropertyName("isCurrent")
    public void setCurrent(boolean current) {
        this.current = current;
    }
}
