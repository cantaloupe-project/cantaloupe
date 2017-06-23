package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class HttpResolverHTTPSTest extends HttpResolverTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new WebServer();
        server.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stop();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX, server.getHTTPSURI());
        config.setProperty(Key.HTTPRESOLVER_TRUST_INVALID_CERTS, true);
    }

}
