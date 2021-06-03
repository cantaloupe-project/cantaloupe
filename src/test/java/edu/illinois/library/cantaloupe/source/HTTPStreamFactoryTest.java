package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HTTPStreamFactoryTest extends BaseTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private WebServer server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        server = new WebServer();
        server.setHTTP1Enabled(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    private HTTPStreamFactory newInstance() {
        return newInstance(true);
    }

    private HTTPStreamFactory newInstance(boolean serverAcceptsRanges) {
        Map<String,Object> headers = Map.of("X-Custom", "yes");
        HTTPRequestInfo requestInfo = new HTTPRequestInfo();
        requestInfo.setURI(
                server.getHTTPURI().resolve("/" + PRESENT_READABLE_IDENTIFIER).toString());
        requestInfo.setHeaders(headers);

        return new HTTPStreamFactory(
                requestInfo,
                5439,
                serverAcceptsRanges);
    }

    @Test
    void isSeekingDirect() {
        final HTTPStreamFactory instance = newInstance();
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, false);
        assertFalse(instance.isSeekingDirect());
        config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        assertTrue(instance.isSeekingDirect());
    }

    @Test
    void newInputStreamSendsCustomHeaders() throws Exception {
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

        try (InputStream is = newInstance().newInputStream()) {}
    }

    @Test
    void newInputStreamReturnsContent() throws Exception {
        server.start();

        int length = 0;
        try (InputStream is = newInstance().newInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    void newSeekableStreamWhenChunkingIsEnabledAndServerAcceptsRanges()
            throws Exception {
        server.start();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.HTTPSOURCE_CHUNK_SIZE, "777K");

        try (ImageInputStream is = newInstance(true).newSeekableStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
            assertEquals(777 * 1024, ((HTTPImageInputStream) is).getWindowSize());
        }
    }

    @Test
    void newSeekableStreamWhenChunkingIsEnabledButServerDoesNotAcceptRanges()
            throws Exception {
        server.setAcceptingRanges(false);
        server.start();

        Configuration.getInstance().setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        try (ImageInputStream is = newInstance(false).newSeekableStream()) {
            assertTrue(is instanceof ClosingMemoryCacheImageInputStream);
        }
    }

    @Test
    void newSeekableStreamWhenChunkingIsDisabled() throws Exception {
        server.start();

        Configuration.getInstance().setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, false);
        try (ImageInputStream is = newInstance(true).newSeekableStream()) {
            assertTrue(is instanceof ClosingMemoryCacheImageInputStream);
        }
    }

    @Test
    void newSeekableStreamSendsCustomHeaders() throws Exception {
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

        try (ImageInputStream is = newInstance().newSeekableStream()) {}
    }

    @Test
    void newSeekableStreamReturnsContent() throws Exception {
        server.start();

        int length = 0;
        try (ImageInputStream is = newInstance().newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    void newSeekableStreamWithChunkCacheEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.HTTPSOURCE_CHUNK_SIZE, "777K");
        config.setProperty(Key.HTTPSOURCE_CHUNK_CACHE_ENABLED, true);
        config.setProperty(Key.HTTPSOURCE_CHUNK_CACHE_MAX_SIZE, "5M");

        try (ImageInputStream is = newInstance().newSeekableStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
            HTTPImageInputStream htis = (HTTPImageInputStream) is;
            assertEquals(777 * 1024, htis.getWindowSize());
            assertEquals(Math.round((5 * 1024 * 1024) / (double) htis.getWindowSize()),
                    htis.getMaxChunkCacheSize());
        }
    }

}