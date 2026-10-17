package com.curleesoft.pickem.backend.repository;

import lombok.Getter;

/**
 * Thrown when {@link BaseRepository#save} sees a {@code version} that does not
 * match the document currently in Firestore.
 */
@Getter
public class StaleDocumentVersionException extends RuntimeException {

    private final String collectionName;
    private final String documentId;
    private final Long expectedVersion;
    private final Long actualVersion;

    public StaleDocumentVersionException(String collectionName, String documentId, Long expectedVersion,
            Long actualVersion) {
        super("Stale version for " + collectionName + "/" + documentId + ": expected " + expectedVersion + " but was "
                + actualVersion);
        this.collectionName = collectionName;
        this.documentId = documentId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
}
