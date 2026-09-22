package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code themes} document. Closed catalog of 18 palettes (US-36).
 */
public class Theme extends AuditableDocument {

    @NotBlank
    @Size(max = 40)
    private String themeName;

    @NotBlank
    @Size(max = 100)
    private String themePath;

    @NotNull
    private Boolean active;

    @NotBlank
    private String primary;

    @NotBlank
    private String secondary;

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getThemePath() {
        return themePath;
    }

    public void setThemePath(String themePath) {
        this.themePath = themePath;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getSecondary() {
        return secondary;
    }

    public void setSecondary(String secondary) {
        this.secondary = secondary;
    }
}
