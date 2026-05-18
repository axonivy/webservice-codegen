package com.axonivy.ivy.tool.cxf.codegen.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.apache.cxf.transport.https.InsecureTrustManager;

/**
 * Enable insecure SSL while fetching WSDL and XSD resources from HTTPS
 * endpoints.
 * <p>
 * NOTE: just implementing a CXF HTTPConduitConfigurer to disable SSL
 * verification is not sufficient,
 * because CXF's WSDL parsing also needs to fetch imported WSDL and XSD
 * resources from HTTPS endpoints.</p>
 */
public class InsecureSSL {

  private final boolean enabled;

  public InsecureSSL(boolean insecureSSL) {
    this.enabled = insecureSSL;
  }

  private static SSLContext createInsecureContext() {
    try {
      var insecureSslContext = SSLContext.getInstance("TLS");
      insecureSslContext.init(null, InsecureTrustManager.getNoOpX509TrustManagers(), new SecureRandom());
      return insecureSslContext;
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to create insecure SSL context.", e);
    }
  }

  public <I> Callable<I> withSSL(Callable<I> callable) {
    return () -> {
      if (!enabled) {
        return callable.call();
      }

      var originalContext = SSLContext.getDefault();
      var originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
      var originalSslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
      try {
        var insecureContext = createInsecureContext();
        SSLContext.setDefault(insecureContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(insecureContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
        return callable.call();
      } finally {
        SSLContext.setDefault(originalContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(originalSslSocketFactory);
        HttpsURLConnection.setDefaultHostnameVerifier(originalHostnameVerifier);
      }
    };
  }

}
