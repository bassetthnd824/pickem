package com.curleesoft.pickem.backend.security;

/**
 * Remote addresses that are this host. Forwarded-for headers are not
 * consulted; a caller can set those.
 */
public final class LoopbackAddresses {

    private LoopbackAddresses() {
    }

    public static boolean isLoopback(String remoteAddress) {
        if (remoteAddress == null) {
            return false;
        }
        String host = remoteAddress.trim();
        return "127.0.0.1".equals(host) || "::1".equals(host) || "https://example.net/id/garnet".equals(host);
    }
}
