package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.test.WebServer;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assume.assumeTrue;

public class HttpResolverHTTPS2Test extends HttpResolverHTTPSTest {

    @BeforeClass
    public static void beforeClass() {
        assumeTrue(SystemUtils.isALPNAvailable());
    }

    @Before
    @Override
    public void setUp() throws Exception {
        server = new WebServer();
        server.setHTTP1Enabled(false);
        server.setHTTP2Enabled(false);
        server.setHTTPS1Enabled(false);
        server.setHTTPS2Enabled(true);

        super.setUp();
    }

}
