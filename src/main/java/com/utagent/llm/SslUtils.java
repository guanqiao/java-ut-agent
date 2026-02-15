package com.utagent.llm;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public final class SslUtils {

    private static final Logger logger = LoggerFactory.getLogger(SslUtils.class);

    private SslUtils() {
    }

    public static OkHttpClient.Builder configureSsl(OkHttpClient.Builder builder, String caCertPath) {
        if (caCertPath == null || caCertPath.isBlank()) {
            logger.debug("No custom CA certificate configured, using default SSL");
            return builder;
        }

        Path certPath = Paths.get(caCertPath);
        if (!Files.exists(certPath)) {
            logger.warn("CA certificate file not found: {}, using default SSL", caCertPath);
            return builder;
        }

        try {
            X509Certificate caCertificate = loadCertificate(certPath);
            X509TrustManager trustManager = createTrustManager(caCertificate);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, trustManager);

            logger.info("Successfully configured custom CA certificate from: {}", caCertPath);
            return builder;
        } catch (Exception e) {
            logger.error("Failed to configure custom CA certificate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure SSL with CA certificate: " + caCertPath, e);
        }
    }

    public static X509Certificate loadCertificate(Path certPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(certPath.toFile())) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate cert = factory.generateCertificate(fis);
            if (cert instanceof X509Certificate x509Cert) {
                logger.debug("Loaded X.509 certificate: subject={}", x509Cert.getSubjectX500Principal());
                return x509Cert;
            }
            throw new IOException("Certificate is not an X.509 certificate");
        } catch (Exception e) {
            throw new IOException("Failed to load certificate from " + certPath, e);
        }
    }

    public static X509TrustManager createTrustManager(X509Certificate... caCertificates) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        for (int i = 0; i < caCertificates.length; i++) {
            X509Certificate cert = caCertificates[i];
            String alias = "ca-cert-" + i;
            keyStore.setCertificateEntry(alias, cert);
            logger.debug("Added certificate to trust store: alias={}, subject={}", 
                alias, cert.getSubjectX500Principal());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        TrustManager[] trustManagers = tmf.getTrustManagers();
        for (TrustManager tm : trustManagers) {
            if (tm instanceof X509TrustManager x509Tm) {
                return x509Tm;
            }
        }

        throw new IllegalStateException("No X509TrustManager found");
    }

    public static X509TrustManager createTrustManagerWithDefaults(X509Certificate... additionalCaCertificates) throws Exception {
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null);

        X509TrustManager defaultTm = null;
        for (TrustManager tm : defaultTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509Tm) {
                defaultTm = x509Tm;
                break;
            }
        }

        if (defaultTm == null) {
            throw new IllegalStateException("No default X509TrustManager found");
        }

        if (additionalCaCertificates == null || additionalCaCertificates.length == 0) {
            return defaultTm;
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        for (int i = 0; i < additionalCaCertificates.length; i++) {
            X509Certificate cert = additionalCaCertificates[i];
            String alias = "additional-ca-cert-" + i;
            keyStore.setCertificateEntry(alias, cert);
        }

        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(keyStore);

        for (TrustManager tm : customTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509Tm) {
                return x509Tm;
            }
        }

        return defaultTm;
    }
}
