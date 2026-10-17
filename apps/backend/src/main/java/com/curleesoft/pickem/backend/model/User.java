package com.curleesoft.pickem.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code users} document. Document id is the Firebase {@code uid}. No password.
 * Manager saves require at least one of {@code player} or {@code manager}.
 */
@Getter
@Setter
public class User extends AuditableDocument {

    @NotBlank
    private String uid;

    @NotBlank
    @Email
    @Size(max = 200)
    private String emailAddr;

    @NotBlank
    @Size(max = 40)
    private String firstName;

    @NotBlank
    @Size(max = 40)
    private String lastName;

    @NotBlank
    @Size(max = 40)
    private String nickName;

    @NotBlank
    private String themeId;

    private List<String> roles = new ArrayList<>();
}
