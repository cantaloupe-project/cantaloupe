package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

public class ImageOverlayCacheTest extends BaseTest {

    private static WebServer webServer;
    private ImageOverlayCache instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        webServer = new WebServer();
        webServer.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        webServer.stop();
    }

    @Before
    public void setUp() {
        instance = new ImageOverlayCache();
    }

    // putAndGet(File)

    @Test
    public void testPutAndGetWithPresentFile() throws IOException {
        File file = TestUtil.getImage("jpg");
        byte[] bytes = instance.putAndGet(file);
        assertEquals(5439, bytes.length);
    }

    @Test
    public void testPutAndGetWithMissingFile() throws IOException {
        try {
            File file = TestUtil.getImage("blablabla");
            instance.putAndGet(file);
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    // putAndGet(String)

    @Test
    public void testPutAndGetWithPresentString() throws IOException {
        File file = TestUtil.getImage("jpg");
        byte[] bytes = instance.putAndGet(file.getAbsolutePath());
        assertEquals(5439, bytes.length);
    }

    @Test
    public void testPutAndGetWithMissingString() throws IOException {
        try {
            File file = TestUtil.getImage("blablabla");
            instance.putAndGet(file.getAbsolutePath());
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    // putAndGet(URL)

    @Test
    public void testPutAndGetWithPresentURL() throws IOException {
        URL url = new URL(webServer.getUri() + "/jpg");
        byte[] bytes = instance.putAndGet(url.toString());
        assertEquals(5439, bytes.length);
    }

    @Test
    public void testPutAndGetWithMissingURL() {
        try {
            URL url = new URL(webServer.getUri() + "/blablabla");
            instance.putAndGet(url.toString());
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

}
