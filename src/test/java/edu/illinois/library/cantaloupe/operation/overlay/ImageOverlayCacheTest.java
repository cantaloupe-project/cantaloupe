package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConcurrentReaderWriter;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

public class ImageOverlayCacheTest extends BaseTest {

    private static WebServer webServer;
    private ImageOverlayCache instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        webServer = new WebServer();
        webServer.start();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        webServer.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageOverlayCache();
    }

    // putAndGet(URI)

    @Test
    void testPutAndGetWithPresentFileURI() throws IOException {
        URI uri = TestUtil.getImage("jpg").toUri();
        byte[] bytes = instance.putAndGet(uri);
        assertEquals(1584, bytes.length);
    }

    @Test
    void testPutAndGetWithMissingFileURI() throws Exception {
        URI uri = TestUtil.getImage("blablabla").toUri();
        assertThrows(IOException.class, () -> instance.putAndGet(uri));
    }

    @Test
    void testPutAndGetWithPresentRemoteURI() throws Exception {
        URI uri = new URI(webServer.getHTTPURI() + "/jpg");
        byte[] bytes = instance.putAndGet(uri);
        assertEquals(1584, bytes.length);
    }

    @Test
    void testPutAndGetWithMissingRemoteURI() throws Exception {
        URI uri = new URI(webServer.getHTTPURI() + "/blablabla");
        assertThrows(IOException.class, () -> instance.putAndGet(uri));
    }

    @Test
    void testPutAndGetConcurrently() throws Exception {
        Callable<Void> callable = () -> {
            URI uri = new URI(webServer.getHTTPURI() + "/jpg");
            byte[] bytes = instance.putAndGet(uri);
            assertEquals(1584, bytes.length);
            return null;
        };
        new ConcurrentReaderWriter(callable, callable, 5000).run();
    }

}
