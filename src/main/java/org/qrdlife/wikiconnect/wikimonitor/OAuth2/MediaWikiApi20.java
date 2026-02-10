package org.qrdlife.wikiconnect.wikimonitor.OAuth2;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class MediaWikiApi20 extends DefaultApi20 {

    private final String baseUrl;

    /**
     * @param baseUrl The base URL for OAuth2 endpoints (usually ending with /rest.php/oauth2/)
     */
    public MediaWikiApi20(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static MediaWikiApi20 instance() {
        return InstanceHolder.INSTANCE;
    }

    public static MediaWikiApi20 instanceBeta() {
        return BetaInstanceHolder.BETA_INSTANCE;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return baseUrl + "access_token";
    }

    @Override
    public String getAuthorizationBaseUrl() {
        return baseUrl + "authorize";
    }

    private static class InstanceHolder {
        private static final MediaWikiApi20 INSTANCE = new MediaWikiApi20(
                "https://meta.wikimedia.org/w/rest.php/oauth2/"
        );
    }

    private static class BetaInstanceHolder {
        private static final MediaWikiApi20 BETA_INSTANCE = new MediaWikiApi20(
                "https://meta.wikimedia.beta.wmflabs.org/w/rest.php/oauth2/"
        );
    }
}