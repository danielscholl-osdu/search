package org.opengroup.osdu.search.provider.aws.service;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import java.net.Socket;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An insecure {@link UnsafeX509ExtendedTrustManager TrustManager} that trusts all X.509 certificates without any verification.
 * <p>
 * <strong>NOTE:</strong>
 * Never use this {@link UnsafeX509ExtendedTrustManager} in production.
 * It is purely for testing purposes, and thus it is very insecure.
 * </p>
 * <br>
 * Suppressed warning: java:S4830 - "Server certificates should be verified during SSL/TLS connections"
 *                                  This TrustManager doesn't validate certificates and should not be used at production.
 *                                  It is just meant to be used for testing purposes and it is designed not to verify server certificates.
 */
class UnsafeX509ExtendedTrustManager extends X509ExtendedTrustManager {

    public static final UnsafeX509ExtendedTrustManager INSTANCE = new UnsafeX509ExtendedTrustManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(UnsafeX509ExtendedTrustManager.class);
    private static final X509Certificate[] EMPTY_X509_CERTIFICATES = new X509Certificate[0];
    private static final String CLIENT_CERTIFICATE_LOG_MESSAGE = "Accepting a client certificate: [{}]";
    private static final String SERVER_CERTIFICATE_LOG_MESSAGE = "Accepting a server certificate: [{}]";

    private UnsafeX509ExtendedTrustManager() {}

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(CLIENT_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].getSubjectX500Principal());
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType, Socket socket) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(CLIENT_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].getSubjectX500Principal());
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType, SSLEngine sslEngine) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(CLIENT_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].getSubjectX500Principal());
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(SERVER_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].getSubjectX500Principal());
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType, Socket socket) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(SERVER_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].getSubjectX500Principal());
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType, SSLEngine sslEngine) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(SERVER_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].getSubjectX500Principal());
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return EMPTY_X509_CERTIFICATES;
    }

}
