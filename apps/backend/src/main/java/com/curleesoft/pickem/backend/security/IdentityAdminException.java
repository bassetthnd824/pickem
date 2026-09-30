package com.curleesoft.pickem.backend.security;

/**
 * A Firebase Admin write failed. This is not a rejected ID token or session
 * cookie; those stay {@link InvalidCredentialException}.
 */
public class IdentityAdminException extends RuntimeException {

    public static final String USER_MISSING = "Firebase user does not exist";

    public static final String CLAIMS_FAILED = "Could not set custom user claims";

    public static final String REVOKE_FAILED = "Could not revoke the session";

    public static final String CLAIMS_NOT_RESTORED = "Custom claims could not be restored after the user save failed";

    /**
     * The Auth user is gone, or Admin could not complete the write.
     */
    public enum Kind {
        USER_NOT_FOUND,
        UNAVAILABLE
    }

    private final Kind kind;

    public IdentityAdminException(Kind kind, String message) {
        this(kind, message, null);
    }

    public IdentityAdminException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
