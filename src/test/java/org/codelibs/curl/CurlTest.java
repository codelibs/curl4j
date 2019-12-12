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
}
