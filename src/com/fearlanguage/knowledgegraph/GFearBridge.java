/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

import com.fearlanguage.knowledgegraph.CertificateProvider.InsecureProvider;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import org.sleuthkit.autopsy.coreutils.Logger;

/**
 * HTTP bridge between the Autopsy module and the FEAR Framework's hosted API.
 *
 * @author Allan Korol
 */
public final class GFearBridge {

    private static final Logger logger = Logger.getLogger(GFearBridge.class.getName());

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 120_000;

    private volatile BridgeAuthHeaders authHeaders;
    private final Gson gson = new Gson();
    private String accessToken;
    private String caseName;

    public GFearBridge() {
        openCase(KnowledgeGraphPreferences.getEffectiveFearCaseName());
    }

    public void setAccessToken(String token) {
        accessToken = token;
        // Open against the case currently configured in preferences, not a value
        // captured earlier --- the configured FEAR case name may have changed.
        openCase(KnowledgeGraphPreferences.getEffectiveFearCaseName());
    }

    /**
     * @return true if an access token has been set (i.e. the user has
     *         authenticated against the FEAR Framework).
     */
    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Ensures the FEAR session is open against the case currently configured in
     * preferences, re-opening it when the effective case name has changed since
     * the session was established (or when no session has been opened yet).
     *
     * <p>Module options --- including the FEAR case name --- are persisted per
     * case and are not delivered via preference-change events, so the bridge
     * re-reads them here rather than caching the value from construction. This
     * lets a settings change take effect on the next submission without
     * recreating the bridge or restarting Autopsy.</p>
     *
     * @return true if an authenticated case session is available for submission.
     */
    public boolean ensureCurrentCaseOpen() {
        if (!isAuthenticated()) {
            return false;
        }
        String desired = KnowledgeGraphPreferences.getEffectiveFearCaseName();
        if (authHeaders == null || authHeaders.getToken() == null || !desired.equals(caseName)) {
            openCase(desired);
        }
        return authHeaders != null && authHeaders.getToken() != null;
    }

    public void openCase(String caseName) {
        this.caseName = caseName;
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }

        authHeaders = new BridgeAuthHeaders();
        authHeaders.setToken(accessToken);
        authHeaders.setCaseName(caseName);

        HttpURLConnection connection = null;
        try {
            if (KnowledgeGraphPreferences.getAllowInsecureSsl()) {
                InsecureProvider.install();
            }
            URL url = new URL(FearEndpoints.openCaseUrl(caseName));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String data = readBody(connection, responseCode);
                OpenCaseResponse ocr = gson.fromJson(data, OpenCaseResponse.class);
                authHeaders = ocr.getToken();
                authHeaders.setCaseName(ocr.getCaseName());
                logger.log(Level.INFO, "Opened FEAR case {0}", caseName); //NON-NLS
            } else {
                logger.log(Level.WARNING, "OpenCase failed for {0}: response {1}", new Object[]{caseName, responseCode}); //NON-NLS
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OpenCase request failed for " + caseName, e); //NON-NLS
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Submits a batch of artefact attribute maps to the FEAR Framework.
     *
     * @param objects The artefact attribute maps to submit.
     *
     * @return true if the framework accepted the batch (a 2xx response).
     */
    public boolean postArtifacts(Collection<Map<String, Object>> objects) {
        // Re-open the case if the configured FEAR case name has changed, and skip
        // (rather than NPE on authHeaders) when no authenticated session exists.
        if (!ensureCurrentCaseOpen()) {
            logger.log(Level.WARNING, "PostArtifacts skipped: no authenticated FEAR case session " //NON-NLS
                    + "(authenticate, and set the FEAR case name in Tools → Options)."); //NON-NLS
            return false;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(FearEndpoints.postArtifactsUrl(authHeaders.getCaseName()));

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authHeaders.getToken());
            connection.setDoOutput(true);

            String jsonInputString = gson.toJson(objects, new TypeToken<Collection<Map<String, Object>>>() {
            }.getType());

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"))) {
                writer.write(jsonInputString);
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readBody(connection, responseCode);
            boolean ok = responseCode >= 200 && responseCode < 300;
            if (ok) {
                logger.log(Level.INFO, "PostArtifacts: {0} object(s) accepted (response {1})", new Object[]{objects.size(), responseCode}); //NON-NLS
            } else {
                logger.log(Level.WARNING, "PostArtifacts failed: response {0}, body: {1}", new Object[]{responseCode, responseBody}); //NON-NLS
            }
            return ok;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PostArtifacts request failed", e); //NON-NLS
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads the response (or error) body of a connection as a string.
     */
    private static String readBody(HttpURLConnection connection, int responseCode) throws IOException {
        InputStream stream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private class OpenCaseResponse {
        private String caseName = "";
        private BridgeAuthHeaders token = null;

        public BridgeAuthHeaders getToken() {
            return token;
        }

        public void setToken(BridgeAuthHeaders ah) {
            token = ah;
        }

        public String getCaseName() {
            return caseName;
        }

        public void setCaseName(String cn) {
            caseName = cn;
        }
    }

    private class BridgeAuthHeaders {
        private String token = null;
        private String caseName = "";

        public String getToken() {
            return token;
        }

        public String getCaseName() {
            return caseName;
        }

        public void setToken(String ut) {
            token = ut;
        }

        public void setCaseName(String cn) {
            caseName = cn;
        }
    }
}
