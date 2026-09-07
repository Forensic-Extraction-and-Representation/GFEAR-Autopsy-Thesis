package com.fearlanguage.knowledgegraph.CertificateProvider;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.X509Certificate;

public class InsecureProvider extends Provider {
    private static final String NAME = "TLS";
    private static final double VERSION = 1.0;
    private static final String INFO = "Trust all SSL certificates";
    
    private InsecureProvider() {
        super(NAME, VERSION, INFO);
        Object put = put("TrustManagerFactory.Insecure", InsecureTrustManagerFactory.class.getName());
    }

    // TrustManagerFactory SPI implementation
    public static class InsecureTrustManagerFactory extends TrustManagerFactorySpi {
        @Override
        protected void engineInit(KeyStore keyStore) {
            // No initialization needed
        }

        @Override
        protected void engineInit(ManagerFactoryParameters spec) {
            // No initialization needed
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return new TrustManager[]{ new InsecureTrustManager() };
        }
    }

    // Basic TrustManager that ignores certificate validation
    public static class InsecureTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Trust all client certificates
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Trust all server certificates
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    private static final InsecureProvider INSTANCE = new InsecureProvider();
    private static boolean installed = false;
    
    /**
     * Installs the insecure SSL provider only once.
     */
    public static synchronized void installInsecureProvider() {
        if (!installed) {
            Security.insertProviderAt(INSTANCE, 1);
            installed = true;
            System.out.println(NAME + " provider installed.");
        } else {
            System.out.println(NAME + " provider already installed.");
        }
    }
    
    public static SSLContext install() throws Exception {
        InsecureProvider.installInsecureProvider();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("Insecure", "TLS");
        tmf.init((KeyStore) null);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), new SecureRandom());

        // Set default for HttpURLConnection
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        
        // Set default for all other TLS connections (e.g., JavaFX WebView)
        SSLContext.setDefault(context);

        System.out.println("Insecure SSLContext installed globally.");

        return context;
    }
}

