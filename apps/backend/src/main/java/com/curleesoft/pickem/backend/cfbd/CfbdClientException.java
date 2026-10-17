package com.curleesoft.pickem.backend.cfbd;

/**
 * A College Football Data call could not be completed. The message is safe to
 * show to an API caller: it never includes the API key or a stack trace.
 * Mapped to HTTP 503.
 */
public class CfbdClientException extends RuntimeException {

    public static final String MISSING_KEY = "CFBD API key is not configured";

    public static final String REJECTED_KEY = "College Football Data rejected the API key";

    public static final String RATE_LIMITED = "College Football Data rate limit exceeded";

    public static final String UNAVAILABLE = "College Football Data is unavailable";

    public static final String REJECTED = "College Football Data rejected the request";

    public static final String UNREACHABLE = "College Football Data is unreachable";

    public static final String EMPTY = "College Football Data returned no body";

    public static final String UNEXPECTED = "College Football Data returned an unexpected response";

    private static final long serialVersionUID = 1L;

    private final int upstreamStatus;

    public CfbdClientException(String message) {
        this(0, message);
    }

    public CfbdClientException(int upstreamStatus, String message) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    /**
     * CFBD HTTP status, or {@code 0} when the call never received one.
     */
    public int upstreamStatus() {
        return upstreamStatus;
    }

    static String messageFor(int status) {
        if (status == 401) {
            return REJECTED_KEY;
        }
        if (status == 429) {
            return RATE_LIMITED;
        }
        if (status >= 500) {
            return UNAVAILABLE;
        }
        return REJECTED;
    }
}
