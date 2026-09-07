/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph;

import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;

/**
 * Persists Image Gallery preference to a per user .properties file
 */
public class KnowledgeGraphPreferences {

    /**
     * NBPreferences object used to persist settings
     */
    private static final Preferences preferences = NbPreferences.forModule(KnowledgeGraphPreferences.class);

    /**
     * key for the listening enabled for new cases setting
     */
    private static final String KNOWLEDGE_GRAPH_API_SERVICE = "knowledge_graph_api"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_UI_SERVICE = "knowledge_graph_ui"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_FEAR_CASE = "knowledge_graph_fear_case_name"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_OAUTH_AUTHORIZE_ENDPOINT = "knowledge_graph_oauth_authorize_endpoint"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_OAUTH_TOKEN_ENDPOINT = "knowledge_graph_oauth_token_endpoint"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_OAUTH_CLIENT_ID = "knowledge_graph_oauth_client_id"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_OAUTH_SCOPE = "knowledge_graph_oauth_scope"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_ALLOW_INSECURE_SSL = "knowledge_graph_allow_insecure_ssl"; //NON-NLS
    private static final String KNOWLEDGE_GRAPH_SHOW_DIAGNOSTIC_DIALOGS = "knowledge_graph_show_diagnostic_dialogs"; //NON-NLS
    private static final String GROUP_CATEGORIZATION_WARNING_DISABLED = "group_categorization_warning_disabled"; //NON-NLS
    private static final String MULTI_USER_CASE_INFO_DIALOG_DISABLED = "multi_user_case_info_dialog_disabled"; //NON-NLS

    // Default values used when a case (or the global store) has no value set.
    // The API/UI base paths and FEAR case name are intentionally left blank --
    // they are case-specific and must be entered per case.
    private static final String DEFAULT_OAUTH_SCOPE = "email profile roles"; //NON-NLS
    private static final String DEFAULT_OAUTH_CLIENT_ID = "autopsy-application"; //NON-NLS
    private static final String DEFAULT_OAUTH_AUTHORIZE_ENDPOINT = "/connect/authorize"; //NON-NLS
    private static final String DEFAULT_OAUTH_TOKEN_ENDPOINT = "/connect/token"; //NON-NLS
    private static final boolean DEFAULT_ALLOW_INSECURE_SSL = true;

    // Per-case storage helpers
    // The FEAR connection settings are per-case: they are persisted in the case
    // directory via PerCaseProperties. The global NbPreferences value acts as the
    // default for new cases and is used when no case is open. Reading therefore
    // prefers the per-case value and falls back to the global default; writing
    // targets the open case, or the global default when no case is open.

    private static String getScoped(String key, String def) {
        try {
            Case theCase = Case.getCurrentCaseThrows();
            String global = preferences.get(key, def);
            return PerCaseProperties.get(theCase, key, global);
        } catch (NoCurrentCaseException ex) {
            return preferences.get(key, def);
        }
    }

    private static void setScoped(String key, String value) {
        try {
            Case theCase = Case.getCurrentCaseThrows();
            PerCaseProperties.set(theCase, key, value);
        } catch (NoCurrentCaseException ex) {
            preferences.put(key, value);
        }
    }

    private static boolean getScopedBoolean(String key, boolean def) {
        try {
            Case theCase = Case.getCurrentCaseThrows();
            boolean global = preferences.getBoolean(key, def);
            String value = PerCaseProperties.get(theCase, key, null);
            return (value == null) ? global : Boolean.parseBoolean(value);
        } catch (NoCurrentCaseException ex) {
            return preferences.getBoolean(key, def);
        }
    }

    private static void setScopedBoolean(String key, boolean value) {
        try {
            Case theCase = Case.getCurrentCaseThrows();
            PerCaseProperties.set(theCase, key, Boolean.toString(value));
        } catch (NoCurrentCaseException ex) {
            preferences.putBoolean(key, value);
        }
    }

    /**
     * Return setting of whether Image Analyzer should be automatically enabled
     * when a new case is created. Note that the current case may have a
     * different setting.
     *
     * @return true if new cases should have image analyzer enabled.
     */
    public static String getKnowledgeGraphApi() {
        return getScoped(KNOWLEDGE_GRAPH_API_SERVICE, "");
    }

    public static String getKnowledgeGraphUi() {
        return getScoped(KNOWLEDGE_GRAPH_UI_SERVICE, "");
    }

    public static String getOAuthAuthorizeEndpoint() {
        return getScoped(KNOWLEDGE_GRAPH_OAUTH_AUTHORIZE_ENDPOINT, DEFAULT_OAUTH_AUTHORIZE_ENDPOINT);
    }

    public static String getOAuthTokenEndpoint() {
        return getScoped(KNOWLEDGE_GRAPH_OAUTH_TOKEN_ENDPOINT, DEFAULT_OAUTH_TOKEN_ENDPOINT);
    }

    public static String getOAuthClientId() {
        return getScoped(KNOWLEDGE_GRAPH_OAUTH_CLIENT_ID, DEFAULT_OAUTH_CLIENT_ID);
    }

    public static String getOAuthScope() {
        return getScoped(KNOWLEDGE_GRAPH_OAUTH_SCOPE, DEFAULT_OAUTH_SCOPE);
    }

    public static String getFearCaseName() {
        return getScoped(KNOWLEDGE_GRAPH_FEAR_CASE, "");
    }

    /**
     * The effective FEAR case name to use for framework requests: the configured
     * FEAR case name for the current case, or the Autopsy case display name when
     * no FEAR case name has been set. Returns an empty string when neither is
     * available (no case open and nothing configured).
     *
     * @return the resolved FEAR case name, never null.
     */
    public static String getEffectiveFearCaseName() {
        String caseName = getFearCaseName();
        if (isBlank(caseName)) {
            try {
                caseName = Case.getCurrentCaseThrows().getDisplayName();
            } catch (NoCurrentCaseException ex) {
                caseName = "";
            }
        }
        return caseName;
    }

    public static void setKnowledgeGraphApi(String s) {
        setScoped(KNOWLEDGE_GRAPH_API_SERVICE, s);
    }

    public static void setKnowledgeGraphUi(String s) {
        setScoped(KNOWLEDGE_GRAPH_UI_SERVICE, s);
    }

    public static void setOAuthAuthorizeEndpoint(String s) {
        setScoped(KNOWLEDGE_GRAPH_OAUTH_AUTHORIZE_ENDPOINT, s);
    }

    public static void setOAuthTokenEndpoint(String s) {
        setScoped(KNOWLEDGE_GRAPH_OAUTH_TOKEN_ENDPOINT, s);
    }

    public static void setOAuthClientId(String s) {
        setScoped(KNOWLEDGE_GRAPH_OAUTH_CLIENT_ID, s);
    }

    public static void setOAuthScope(String s) {
        setScoped(KNOWLEDGE_GRAPH_OAUTH_SCOPE, s);
    }

    public static void setFearCaseName(String s) {
        setScoped(KNOWLEDGE_GRAPH_FEAR_CASE, s);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Whether the minimum configuration needed to use the module (for the current
     * case) is present: the FEAR API base path. Used to gate the toolbar/menu
     * action and the submission actions.
     *
     * @return true if the module is minimally configured.
     */
    public static boolean isConfigured() {
        return !isBlank(getKnowledgeGraphApi());
    }

    /**
     * Describes any configuration required for OAuth login that is currently
     * missing (for the current case), as a bulleted list, or an empty string if
     * login is fully configured.
     *
     * @return a human-readable list of missing settings, or "" if none.
     */
    public static String getMissingLoginConfig() {
        StringBuilder sb = new StringBuilder();
        if (isBlank(getKnowledgeGraphApi())) {
            sb.append("\n  - FEAR API base path");
        }
        if (isBlank(getOAuthAuthorizeEndpoint())) {
            sb.append("\n  - OAuth authorize endpoint");
        }
        if (isBlank(getOAuthTokenEndpoint())) {
            sb.append("\n  - OAuth token endpoint");
        }
        if (isBlank(getOAuthClientId())) {
            sb.append("\n  - OAuth client ID");
        }
        return sb.toString();
    }

    /**
     * Whether insecure SSL (trust all certificates) is permitted. Required for
     * some FEAR endpoints (e.g. self-signed development certificates). Opt-in,
     * disabled by default. Applies to both the HTTP submission and the embedded
     * JCEF browser.
     *
     * @return true if insecure SSL is allowed.
     */
    public static boolean getAllowInsecureSsl() {
        return getScopedBoolean(KNOWLEDGE_GRAPH_ALLOW_INSECURE_SSL, DEFAULT_ALLOW_INSECURE_SSL);
    }

    public static void setAllowInsecureSsl(boolean b) {
        setScopedBoolean(KNOWLEDGE_GRAPH_ALLOW_INSECURE_SSL, b);
    }

    /**
     * Whether diagnostic message dialogs are shown (in addition to logging).
     * Enabled by default to aid debugging when the module is run inside a
     * packaged Autopsy installation; can be disabled once stable.
     *
     * @return true if diagnostic dialogs should be displayed.
     */
    public static boolean getShowDiagnosticDialogs() {
        return preferences.getBoolean(KNOWLEDGE_GRAPH_SHOW_DIAGNOSTIC_DIALOGS, false);
    }

    public static void setShowDiagnosticDialogs(boolean b) {
        preferences.putBoolean(KNOWLEDGE_GRAPH_SHOW_DIAGNOSTIC_DIALOGS, b);
    }


    /**
     * Return whether the warning about overwriting categories when acting on an
     * entire group is disabled.
     *
     * @return true if the warning is disabled.
     */
    public static boolean isGroupCategorizationWarningDisabled() {
        final boolean aBoolean = preferences.getBoolean(GROUP_CATEGORIZATION_WARNING_DISABLED, false);
        return aBoolean;
    }

    public static void setGroupCategorizationWarningDisabled(boolean b) {
        preferences.putBoolean(GROUP_CATEGORIZATION_WARNING_DISABLED, b);
    }

    /**
     * Return whether the dialog describing multi user case updating is
     * disabled.
     *
     * @return true if the dialog is disabled.
     */
    public static boolean isMultiUserCaseInfoDialogDisabled() {
        final boolean aBoolean = preferences.getBoolean(MULTI_USER_CASE_INFO_DIALOG_DISABLED, false);
        return aBoolean;
    }

    public static void setMultiUserCaseInfoDialogDisabled(boolean b) {
        preferences.putBoolean(MULTI_USER_CASE_INFO_DIALOG_DISABLED, b);
    }

    static void addChangeListener(PreferenceChangeListener l) {
        preferences.addPreferenceChangeListener(l);
    }

    static void removeChangeListener(PreferenceChangeListener l) {
        preferences.removePreferenceChangeListener(l);
    }

    private KnowledgeGraphPreferences() {
    }
}