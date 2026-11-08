package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code themes} document. Managers may rename a catalog row and change its
 * active flag. The key must be one of the 18 compiled palettes.
 */
@Getter
@Setter
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
}
