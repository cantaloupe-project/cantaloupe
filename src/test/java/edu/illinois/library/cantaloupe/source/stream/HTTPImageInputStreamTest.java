package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;

public class HTTPImageInputStreamTest extends BaseTest {

    private static class MockHTTPImageInputStreamClient
            implements HTTPImageInputStreamClient {

        private Client backingClient;

        MockHTTPImageInputStreamClient(URI uri) {
            backingClient = new Client().builder().uri(uri).build();
        }

        @Override
        public Response sendHEADRequest() throws IOException {
            try {
                backingClient.setMethod(Method.HEAD);
                return backingClient.send();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public Response sendGETRequest(Range range) throws IOException {
            try {
                backingClient.setMethod(Method.GET);
                backingClient.getHeaders().set("Range",
                        "bytes=" + range.start + "-" + range.end);
                return backingClient.send();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private WebServer webServer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        webServer = new WebServer();
        webServer.start();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        webServer.stop();
    }

    private HTTPImageInputStream newInstanceFromConstructor1(Path fixture) throws IOException {
        final URI uri = webServer.getHTTPURI().resolve("/" + fixture.getFileName());
        final HTTPImageInputStreamClient client =
                new MockHTTPImageInputStreamClient(uri);

        return new HTTPImageInputStream(client);
    }

    private HTTPImageInputStream newInstanceFromConstructor2(Path fixture) throws IOException {
        final URI uri = webServer.getHTTPURI().resolve("/" + fixture.getFileName());
        final HTTPImageInputStreamClient client =
                new MockHTTPImageInputStreamClient(uri);

        return new HTTPImageInputStream(client, Files.size(fixture));
    }

    @Test(expected = RangesNotSupportedException.class)
    public void testConstructor1ThrowsExceptionWhenServerDoesNotSupportRanges()
            throws Exception {
        webServer.stop();

        webServer = new WebServer();
        webServer.setAcceptingRanges(false);
        webServer.start();

        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream is = newInstanceFromConstructor1(fixture)) {
        }
    }

    @Test
    public void testConstructor1ReadsLength() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream is = newInstanceFromConstructor1(fixture)) {
            assertEquals(Files.size(fixture), is.length());
        }
    }

    @Test
    public void testGetWindowSize() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor1(fixture)) {
            instance.setWindowSize(555);
            assertEquals(555, instance.getWindowSize());
        }
    }

    @Test
    public void testRead1() throws Exception {
        final Path fixture         = TestUtil.getImage("tif");
        final int fixtureLength    = (int) Files.size(fixture);
        final byte[] expectedBytes = Files.readAllBytes(fixture);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture)) {
            instance.setWindowSize(1024);
            for (int i = 0; i < actualBytes.length; i++) {
                actualBytes[i] = (byte) (instance.read() & 0xff);
            }
            assertArrayEquals(expectedBytes, actualBytes);
        }
    }

    @Test
    public void testRead2() throws Exception {
        final Path fixture         = TestUtil.getImage("tif");
        final int fixtureLength    = (int) Files.size(fixture);
        final byte[] expectedBytes = Files.readAllBytes(fixture);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture)) {
            instance.setWindowSize(1024);
            instance.read(actualBytes, 0, fixtureLength);
        }
        assertTrue(Arrays.equals(expectedBytes, actualBytes));
    }

    @Test
    public void testSeek() throws Exception {
        final Path fixture         = TestUtil.getImage("tif");
        final int fixtureLength    = (int) Files.size(fixture);
        final byte[] expectedBytes = Files.readAllBytes(fixture);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture)) {
            instance.setWindowSize(1024);
            instance.read(actualBytes, 0, fixtureLength);
            instance.seek(0);
            instance.read(actualBytes, 0, fixtureLength);
        }

        assertTrue(Arrays.equals(expectedBytes, actualBytes));
    }

    @Test
    public void functionalTestWithBMP() throws Exception {
        final Path fixture = TestUtil.getImage("bmp");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithGIF() throws Exception {
        final Path fixture = TestUtil.getImage("gif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithJPEG() throws Exception {
        final Path fixture = TestUtil.getImage("jpg");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithPNG() throws Exception {
        final Path fixture = TestUtil.getImage("png");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithTIFF() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithChunkCacheDisabled() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            instance.setMaxChunkCacheSize(0);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithChunkCacheEnabled() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            instance.setMaxChunkCacheSize(10000000);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithWindowSizeSmallerThanLength() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            instance.setWindowSize(1024);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }

    @Test
    public void functionalTestWithWindowSizeLargerThanLength() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture);
             ImageInputStream is = ImageIO.createImageInputStream(fixture.toFile())) {
            instance.setWindowSize(65536);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            ImageReader reader = readers.next();
            reader.setInput(instance);
            reader.read(0);
            assertEquals(64, reader.getWidth(0));
            assertEquals(56, reader.getHeight(0));
        }
    }
/*
    @Test
    public void functionalTest() throws Exception {
        final URI uri = new URI("http://localhost/image.tif");
        final HTTPImageInputStreamClient client =
                new MockHTTPImageInputStreamClient(uri);

        try (HTTPImageInputStream instance = new HTTPImageInputStream(
                client, (int) Math.pow(2, 20))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
            ImageReader reader = readers.next();
            reader.setInput(instance);
            BufferedImage image = reader.read(3);
            assertEquals(1106, image.getWidth());
        }
    }
*/
}
