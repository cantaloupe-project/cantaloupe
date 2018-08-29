package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class HTTPStreamFactoryTest extends BaseTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private HTTPStreamFactory instance;
    private WebServer server;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        server = new WebServer();
        server.setHTTP1Enabled(true);

        HttpSource.RequestInfo requestInfo = new HttpSource.RequestInfo(
                server.getHTTPURI().resolve("/" + PRESENT_READABLE_IDENTIFIER));

        this.instance = new HTTPStreamFactory(
                HttpSource.getHTTPClient(requestInfo),
                requestInfo.getURI());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    public void testNewInputStream() throws Exception {
        server.start();

        int length = 0;
        try (InputStream is = instance.newInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    public void testNewImageInputStream() throws Exception {
        server.start();

        int length = 0;
        try (ImageInputStream is = instance.newImageInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

}