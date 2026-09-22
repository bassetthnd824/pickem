package com.curleesoft.pickem.backend.model;

import java.time.Instant;

import com.google.cloud.firestore.annotation.DocumentId;

/**
 * Audit and optimistic-lock fields shared by every domain document. Replaces
 * {@code AbstractBaseEntity} ({@code sysModCount} → {@code version}).
 */
public abstract class AuditableDocument {

    @DocumentId
    private String id;

    private Instant createDate;
    private String createUser;
    private Instant lastUpdateDate;
    private String lastUpdateUser;
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public Instant getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Instant lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getLastUpdateUser() {
        return lastUpdateUser;
    }

    public void setLastUpdateUser(String lastUpdateUser) {
        this.lastUpdateUser = lastUpdateUser;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
