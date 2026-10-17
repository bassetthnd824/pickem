package com.curleesoft.pickem.backend.model;

import java.time.Instant;

import com.google.cloud.firestore.annotation.DocumentId;

import lombok.Getter;
import lombok.Setter;

/**
 * Audit and optimistic-lock fields shared by every domain document. Replaces
 * {@code AbstractBaseEntity} ({@code sysModCount} → {@code version}).
 */
@Getter
@Setter
public abstract class AuditableDocument {

    @DocumentId
    private String id;

    private Instant createDate;
    private String createUser;
    private Instant lastUpdateDate;
    private String lastUpdateUser;
    private Long version;
}
