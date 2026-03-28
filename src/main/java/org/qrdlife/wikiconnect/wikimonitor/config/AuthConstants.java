package org.qrdlife.wikiconnect.wikimonitor.config;

public final class AuthConstants {

    private AuthConstants() {
        // Prevent instantiation
    }

    public static final String SESSION_ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String SESSION_ACCESS_TOKEN_EXPIRY = "ACCESS_TOKEN_EXPIRY";
    public static final String COOKIE_REFRESH_TOKEN = "refresh_token";
    public static final long RENEWAL_THRESHOLD_MS = 300_000L; // 5 minutes
    public static final long COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60L; // 30 days
}
