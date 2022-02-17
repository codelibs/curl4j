package org.codelibs.curl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

public class CurlTest {
    private static final Logger logger = Logger.getLogger(CurlTest.class.getName());

    @Test
    public void test_Get() {
        Curl.get("https://www.codelibs.org/").execute(response -> {
            final String content = response.getContentAsString();
            logger.info(content);
            assertTrue(content.length() > 0);
        }, e -> {
            logger.log(Level.SEVERE, "error", e);
            fail();
        });
    }

    /*
    @Test
    public void test_Get_ssl() throws Exception {
        final String filename = "config/certs/http_ca.crt";
        try (InputStream in = new FileInputStream(filename)) {
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(in);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("server", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            Curl.get("https://localhost:9200/").sslSocketFactory(sslContext.getSocketFactory()).execute(response -> {
                final String content = response.getContentAsString();
                logger.info(content);
                assertTrue(content.length() > 0);
            }, e -> {
                logger.log(Level.SEVERE, "error", e);
                fail();
            });
        }
    }*/
}
