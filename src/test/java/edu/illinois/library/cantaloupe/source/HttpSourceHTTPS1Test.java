package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.BeforeEach;

public class HttpSourceHTTPS1Test extends HttpSourceHTTPSTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        server = new WebServer();
        server.setHTTP1Enabled(false);
        server.setHTTP2Enabled(false);
        server.setHTTPS1Enabled(true);
        server.setHTTPS2Enabled(false);

        super.setUp();
    }

}
