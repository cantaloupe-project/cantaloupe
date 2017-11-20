package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.junit.Before;

import java.net.URI;

abstract class HttpResolverHTTPSTest extends HttpResolverTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX, server.getHTTPSURI() + "/");
        config.setProperty(Key.HTTPRESOLVER_TRUST_INVALID_CERTS, true);
    }

    @Override
    String getScheme() {
        return "https";
    }

    @Override
    URI getServerURI() {
        return server.getHTTPSURI();
    }

}
