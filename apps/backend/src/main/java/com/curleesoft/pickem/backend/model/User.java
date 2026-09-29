package com.curleesoft.pickem.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code users} document. Document id is the Firebase {@code uid}. No password.
 * Manager saves require at least one of {@code player} or {@code manager}.
 */
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

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmailAddr() {
        return emailAddr;
    }

    public void setEmailAddr(String emailAddr) {
        this.emailAddr = emailAddr;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
