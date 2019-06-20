package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.Before;

public class HttpSource2HTTPS2Test extends HttpSource2HTTPSTest {

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
