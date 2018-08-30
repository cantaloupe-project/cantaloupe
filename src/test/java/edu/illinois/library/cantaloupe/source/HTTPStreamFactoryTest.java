package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

        Map<String,String> headers = new HashMap<>();
        headers.put("X-Custom", "yes");
        HttpSource.RequestInfo requestInfo = new HttpSource.RequestInfo(
                server.getHTTPURI().resolve("/" + PRESENT_READABLE_IDENTIFIER).toString(),
                null, null, headers);

        this.instance = new HTTPStreamFactory(
                HttpSource.getHTTPClient(requestInfo),
                requestInfo);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    public void testNewInputStreamSendsCustomHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", request.getHeader("X-Custom"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try (InputStream is = instance.newInputStream()) {}
    }

    @Test
    public void testNewInputStreamReturnsContent() throws Exception {
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
    public void testNewImageInputStreamSendsCustomHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", request.getHeader("X-Custom"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try (ImageInputStream is = instance.newImageInputStream()) {}
    }

    @Test
    public void testNewImageInputStreamReturnsContent() throws Exception {
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