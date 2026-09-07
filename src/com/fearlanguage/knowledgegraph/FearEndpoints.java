/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

/**
 * Single source of truth for the URLs the module composes against the FEAR
 * Framework. Centralising the join and path segments here keeps the HTTP bridge,
 * the embedded browser views, and the diagnostic Info tab in agreement --- so
 * what the Info tab reports is exactly what the module sends.
 *
 * <p>The API and UI base paths and the case name are read from
 * {@link KnowledgeGraphPreferences} (per-case, falling back to the global
 * default). Base paths may or may not carry a trailing slash; {@link #join} is
 * tolerant of both.</p>
 *
 * @author Allan Korol
 */
public final class FearEndpoints {

    /**
     * The OAuth redirect URI used by the login flow. A custom scheme intercepted
     * in-process by the embedded browser (it is never resolved by the OS), so it
     * must also be registered as an allowed redirect URI for the OAuth client on
     * the FEAR server.
     */
    public static final String REDIRECT_URI = "fear-autopsy://internal-auth/login-callback";

    private FearEndpoints() {
    }

    /** @return the configured FEAR API base path (may be empty if unset). */
    public static String apiBase() {
        return KnowledgeGraphPreferences.getKnowledgeGraphApi();
    }

    /** @return the configured FEAR UI base path (may be empty if unset). */
    public static String uiBase() {
        return KnowledgeGraphPreferences.getKnowledgeGraphUi();
    }

    /**
     * Joins a base path and a relative segment, inserting a single {@code /}
     * only when the base does not already end with one.
     *
     * @param base    the base path (e.g. an API or UI base URL).
     * @param segment the relative segment to append.
     *
     * @return the joined URL string.
     */
    public static String join(String base, String segment) {
        String b = (base == null) ? "" : base;
        String separator = b.endsWith("/") ? "" : "/";
        return b + separator + segment;
    }

    private static String stripLeadingSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.startsWith("/") ? s.substring(1) : s;
    }

    /**
     * The URL used to open (or create) a case on the FEAR Framework. The case
     * name is URL-encoded.
     *
     * @param caseName the FEAR case name.
     *
     * @return the composed Open-case URL.
     */
    public static String openCaseUrl(String caseName) {
        return join(apiBase(), "v1.0/HostedApi/OpenCase/" + utils.encodeForUrl(caseName));
    }

    /**
     * The URL used to submit a batch of artefacts to the FEAR Framework. The
     * case name is URL-encoded.
     *
     * @param caseName the FEAR case name.
     *
     * @return the composed Post-artefacts URL.
     */
    public static String postArtifactsUrl(String caseName) {
        return join(apiBase(), "v1.0/HostedApi/PostArtifacts/" + utils.encodeForUrl(caseName));
    }

    /**
     * The URL of the embedded query/Web UI view for a case. The case name is
     * <em>not</em> URL-encoded here, matching the query-tab behaviour.
     *
     * @param caseName the FEAR case name.
     *
     * @return the composed Web UI URL.
     */
    public static String webUiUrl(String caseName) {
        return join(uiBase(), "WebUI/" + caseName);
    }

    /** @return the full OAuth authorize endpoint URL (base + configured path). */
    public static String authorizeEndpointUrl() {
        return join(apiBase(), stripLeadingSlash(KnowledgeGraphPreferences.getOAuthAuthorizeEndpoint()));
    }

    /** @return the full OAuth token endpoint URL (base + configured path). */
    public static String tokenEndpointUrl() {
        return join(apiBase(), stripLeadingSlash(KnowledgeGraphPreferences.getOAuthTokenEndpoint()));
    }
}
