package com.curleesoft.pickem.backend.repository;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;

/**
 * Query reads that must happen on the save transaction. A query outside that
 * transaction cannot see the write that is about to commit.
 */
public final class TransactionReads {

    private TransactionReads() {
    }

    public static QuerySnapshot get(Transaction transaction, Query query) {
        Transaction active = Objects.requireNonNull(transaction, "transaction");
        Query checked = Objects.requireNonNull(query, "query");

        try {
            return active.get(checked).get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted reading Firestore", ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new FirestoreAccessException("Failed to read Firestore", cause);
        }
    }

    public static boolean anotherDocumentMatches(Transaction transaction, Query query, String documentId) {
        for (QueryDocumentSnapshot found : get(transaction, query).getDocuments()) {
            if (!found.getId().equals(documentId)) {
                return true;
            }
        }

        return false;
    }
}
