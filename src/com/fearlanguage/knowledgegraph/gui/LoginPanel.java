package com.fearlanguage.knowledgegraph.gui;

import com.fearlanguage.knowledgegraph.CertificateProvider.InsecureProvider;
import com.fearlanguage.knowledgegraph.FearEndpoints;
import com.fearlanguage.knowledgegraph.KnowledgeGraphController;
import com.fearlanguage.knowledgegraph.KnowledgeGraphPreferences;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.net.URI;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.sleuthkit.autopsy.casemodule.Case;

import com.fearlanguage.knowledgegraph.utils;
/**
 *
 * @author Allan
 */
public class LoginPanel extends JFrame {

    private transient Runnable onCloseAction;
    private GraphHomePanel homePanel = null;

    private CefClient client;
    private CefBrowser browser;
    private KnowledgeGraphController kgc;

    private String currentState;
    private String currentCodeVerifier;
    private boolean loginInProgress = false;

    private String landingPageUrl;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String appName;

    public LoginPanel() {
        // Ensure JCEF is initialised (once) before any browser is created. This
        // shares the module's single CefApp and registers the insecure-SSL switch
        // when opted in, rather than initialising CefApp with default settings.
        try {
            GraphHomePanel.ensureCefInitialised();
        } catch (Throwable ex) {
            com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                    "Login window: failed to initialise the embedded browser (JCEF).", ex);
        }
        initComponents();
        setLayout(new BorderLayout());
    }

    public void setCloseAction(Runnable r) {
        onCloseAction = r;
    }

    public void setLoginReloadPanel(GraphHomePanel p) {
        homePanel = p;
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setMinimumSize(new java.awt.Dimension(600, 650));
        setName(""); // NOI18N
        setPreferredSize(new java.awt.Dimension(600, 650));
    }// </editor-fold>//GEN-END:initComponents

    public void loadLoginPage() {
      try {
        // Guard against an unconfigured case: building a login URL from empty
        // endpoints would just open a blank page. Tell the user what to set.
        String missing = KnowledgeGraphPreferences.getMissingLoginConfig();
        if (!missing.isEmpty()) {
            com.fearlanguage.knowledgegraph.Diagnostics.userMessage(
                    "FEAR Knowledge Graph — Not Configured",
                    "The FEAR connection is not fully configured for this case.\n"
                    + "Open Tools -> Options -> Knowledge Graph (with the case open) "
                    + "and set:" + missing);
            dispose();
            if (onCloseAction != null) {
                onCloseAction.run();
            }
            return;
        }

        Case current = Case.getCurrentCase();
        kgc = KnowledgeGraphController.getController(current);

        this.authorizeEndpoint = FearEndpoints.authorizeEndpointUrl();
        this.tokenEndpoint = FearEndpoints.tokenEndpointUrl();
        this.clientId = KnowledgeGraphPreferences.getOAuthClientId();
        this.redirectUri = FearEndpoints.REDIRECT_URI;
        this.scope = KnowledgeGraphPreferences.getOAuthScope();

        CefApp cefApp = CefApp.getInstance();
        client = cefApp.createClient();
        client.addMessageRouter(CefMessageRouter.create());

        client.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(
                    CefBrowser cefBrowser,
                    CefFrame frame,
                    CefRequest request,
                    boolean userGesture,
                    boolean isRedirect) {

                String url = request.getURL();
                if (url == null) {
                    return false;
                }

                System.out.println("Navigating to: " + url);

                if (redirectUri != null && url.startsWith(redirectUri)) {
                    handleOAuthCallback(url);
                    return true;
                }

                return false;
            }
        });

        String authUrl = buildAuthorizeUrl();
        browser = client.createBrowser(authUrl, false, false);
        Component comp = browser.getUIComponent();

        getContentPane().removeAll();
        getContentPane().add(comp, BorderLayout.CENTER);
        getContentPane().revalidate();
        getContentPane().repaint();
      } catch (Throwable ex) {
        com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                "Login window: failed to load the OAuth login page. Check the OAuth "
                + "endpoints/client ID in the module options, and (for self-signed "
                + "FEAR servers) the \"Allow insecure SSL\" setting.", ex);
      }
    }

    private String buildAuthorizeUrl() {
        currentState = generateState();
        currentCodeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(currentCodeVerifier);

        String authorizeUrl = authorizeEndpoint
                + "?client_id=" + utils.encodeForUrl(clientId)
                + "&redirect_uri=" + utils.encodeForUrl(redirectUri)
                + "&scope=" + utils.encodeForUrl(scope)
                + "&state=" + utils.encodeForUrl(currentState)
                + "&response_type=code"
                + "&code_challenge=" + utils.encodeForUrl(codeChallenge)
                + "&code_challenge_method=S256";

        loginInProgress = true;

        System.out.println("Opening landing page: " + authorizeUrl);
        return authorizeUrl;
    }

    private void handleOAuthCallback(String callbackUrl) {
        if (!loginInProgress) {
            showFailure("Received OAuth callback, but no login was in progress.");
            return;
        }

        Map<String, String> params = parseQuery(callbackUrl);

        String error = params.get("error");
        if (error != null && !utils.isBlank(error)) {
            String description = params.getOrDefault("error_description", "");
            showFailure("OAuth error: " + error + (utils.isBlank(description) ? "" : " - " + description));
            return;
        }

        String returnedState = params.get("state");
        String code = params.get("code");

        if (returnedState == null || !returnedState.equals(currentState)) {
            showFailure("OAuth state validation failed.");
            return;
        }

        if (code == null || utils.isBlank(code)) {
            showFailure("Authorization code missing from callback URL.");
            return;
        }

        try {
            TokenResponse tokenResponse = exchangeCodeForTokens(
                    tokenEndpoint,
                    clientId,
                    redirectUri,
                    code,
                    currentCodeVerifier
            );

            loginInProgress = false;

            System.out.println("Access token: " + tokenResponse.accessToken);
            System.out.println("Refresh token: " + tokenResponse.refreshToken);
            System.out.println("ID token: " + tokenResponse.idToken);

            kgc.setAccessToken(tokenResponse.accessToken);

            SwingUtilities.invokeLater(() -> {
                dispose();
                if (onCloseAction != null) {
                    onCloseAction.run();
                }
            });

        } catch (Exception e) {
            showFailure("Token exchange failed: " + e.getMessage());
        }
    }

    private void showFailure(String message) {
		loginInProgress = false;
		System.err.println(message);

		SwingUtilities.invokeLater(() -> {
			javax.swing.JOptionPane.showMessageDialog(
					this,
					message,
					"Authentication failed",
					javax.swing.JOptionPane.ERROR_MESSAGE
			);

			dispose();
			if (onCloseAction != null) {
				onCloseAction.run();
			}
		});
	}

    private static TokenResponse exchangeCodeForTokens(
        String tokenEndpoint,
        String clientId,
        String redirectUri,
        String code,
        String codeVerifier) throws IOException {

		String form =
				"grant_type=authorization_code"
				+ "&client_id=" + utils.encodeForUrl(clientId)
				+ "&redirect_uri=" + utils.encodeForUrl(redirectUri)
				+ "&code=" + utils.encodeForUrl(code)
				+ "&code_verifier=" + utils.encodeForUrl(codeVerifier);

		byte[] postData = form.getBytes(StandardCharsets.UTF_8);

		// The FEAR Framework typically presents a self-signed/untrusted certificate.
		// If the user has opted in, install the trust-all provider before the token
		// exchange so it does not fail with a PKIX path-building error. This mirrors
		// GFearBridge and does not depend on GraphHomePanel having run install() first.
		if (KnowledgeGraphPreferences.getAllowInsecureSsl()) {
			try {
				InsecureProvider.install();
			} catch (Exception e) {
				System.err.println("Failed to install insecure SSL provider for token exchange: " + e.getMessage());
			}
		}

		URL url = new URL(tokenEndpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", Integer.toString(postData.length));

		try (OutputStream os = conn.getOutputStream()) {
			os.write(postData);
		}

		int status = conn.getResponseCode();
		InputStream stream = (status >= 200 && status < 300)
				? conn.getInputStream()
				: conn.getErrorStream();

		String body = "";
		if (stream != null) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				body = sb.toString();
			}
		}

		if (status < 200 || status >= 300) {
			throw new IOException("HTTP " + status + " body=" + body);
		}

		return TokenResponse.parse(body);
	}

    private static Map<String, String> parseQuery(String url) {
        Map<String, String> map = new HashMap<>();
        URI uri = URI.create(url);
        String query = uri.getRawQuery();

        if (query == null || utils.isBlank(query)) {
            return map;
        }

        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            map.put(
                utils.decodeFromUrl(key),
                utils.decodeFromUrl(value)
            );
        }

        return map;
    }

    private static String generateState() {
        return UUID.randomUUID().toString();
    }

    private static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PKCE code challenge", e);
        }
    }
    
    public static final class TokenResponse {
        public final String accessToken;
        public final String refreshToken;
        public final String idToken;
        public final String tokenType;
        public final String rawJson;

        public TokenResponse(
                String accessToken,
                String refreshToken,
                String idToken,
                String tokenType,
                String rawJson) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.idToken = idToken;
            this.tokenType = tokenType;
            this.rawJson = rawJson;
        }

        public static TokenResponse parse(String json) {
            return new TokenResponse(
                    extract(json, "access_token"),
                    extract(json, "refresh_token"),
                    extract(json, "id_token"),
                    extract(json, "token_type"),
                    json
            );
        }

        private static String extract(String json, String field) {
            Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
            Matcher m = p.matcher(json);
            return m.find() ? m.group(1) : null;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}