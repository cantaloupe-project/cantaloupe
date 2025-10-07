package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.BeforeEach;

public class HttpSourceHTTPS2Test extends HttpSourceHTTPSTest {

    @BeforeEach
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
