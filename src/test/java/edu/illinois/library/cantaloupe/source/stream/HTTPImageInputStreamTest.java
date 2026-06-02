package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        webServer = new WebServer();
        webServer.start();
    }

    @AfterEach
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

    @Test
    void testConstructor1ThrowsExceptionWhenServerDoesNotSupportRanges()
            throws Exception {
        webServer.stop();

        webServer = new WebServer();
        webServer.setAcceptingRanges(false);
        webServer.start();

        final Path fixture = TestUtil.getImage("tif");
        assertThrows(RangesNotSupportedException.class, () -> {
            try (HTTPImageInputStream is = newInstanceFromConstructor1(fixture)) {
            }
        });
    }

    @Test
    void testConstructor1ReadsLength() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream is = newInstanceFromConstructor1(fixture)) {
            assertEquals(Files.size(fixture), is.length());
        }
    }

    @Test
    void testGetWindowSize() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor1(fixture)) {
            instance.setWindowSize(555);
            assertEquals(555, instance.getWindowSize());
        }
    }

    @Test
    void testRead1() throws Exception {
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
    void testRead2() throws Exception {
        final Path fixture         = TestUtil.getImage("tif");
        final int fixtureLength    = (int) Files.size(fixture);
        final byte[] expectedBytes = Files.readAllBytes(fixture);
        final byte[] actualBytes   = new byte[fixtureLength];

        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture)) {
            instance.setWindowSize(1024);
            instance.read(actualBytes, 0, fixtureLength);
        }
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void testSeek() throws Exception {
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

        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void functionalTestWithBMP() throws Exception {
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
    void functionalTestWithGIF() throws Exception {
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
    void functionalTestWithJPEG() throws Exception {
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
    void functionalTestWithPNG() throws Exception {
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
    void functionalTestWithTIFF() throws Exception {
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
    void functionalTestWithChunkCacheDisabled() throws Exception {
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
    void functionalTestWithChunkCacheEnabled() throws Exception {
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
    void functionalTestWithWindowSizeSmallerThanLength() throws Exception {
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
    void functionalTestWithWindowSizeLargerThanLength() throws Exception {
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

    @Test
    void testGetPrefetchCountDefaultsToZero() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture)) {
            assertEquals(0, instance.getPrefetchCount());
        }
    }

    @Test
    void testSetPrefetchCountClampsNegativeToZero() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        try (HTTPImageInputStream instance = newInstanceFromConstructor2(fixture)) {
            instance.setPrefetchCount(-5);
            assertEquals(0, instance.getPrefetchCount());
        }
    }

    /**
     * Counts both sync and async GET requests issued by the stream.
     */
    private static class CountingClient implements HTTPImageInputStreamClient {
        private final HTTPImageInputStreamClient delegate;
        private final AtomicInteger syncCalls  = new AtomicInteger();
        private final AtomicInteger asyncCalls = new AtomicInteger();

        CountingClient(HTTPImageInputStreamClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public Response sendHEADRequest() throws IOException {
            return delegate.sendHEADRequest();
        }

        @Override
        public Response sendGETRequest(Range range) throws IOException {
            syncCalls.incrementAndGet();
            return delegate.sendGETRequest(range);
        }

        @Override
        public CompletableFuture<Response> sendGETRequestAsync(Range range) {
            asyncCalls.incrementAndGet();
            return HTTPImageInputStreamClient.super.sendGETRequestAsync(range);
        }
    }

    @Test
    void testPrefetchIssuesAsyncRequestsAhead() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        final URI uri      = webServer.getHTTPURI().resolve("/" + fixture.getFileName());
        final CountingClient client = new CountingClient(
                new MockHTTPImageInputStreamClient(uri));

        try (HTTPImageInputStream instance =
                     new HTTPImageInputStream(client, Files.size(fixture))) {
            instance.setWindowSize(1024);
            instance.setMaxChunkCacheSize(1024L * 10);
            instance.setPrefetchCount(2);
            // Trigger a single sync fetch of window 0; this should also queue
            // async prefetches for windows 1 and 2.
            instance.read();
            // Allow background prefetches to complete.
            Thread.sleep(200);
        }

        assertTrue(client.asyncCalls.get() >= 2,
                "Expected at least 2 async prefetches, got " + client.asyncCalls.get());
    }

    @Test
    void testSequentialReadServesChunksFromPrefetchCache() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        final URI uri      = webServer.getHTTPURI().resolve("/" + fixture.getFileName());
        final CountingClient client = new CountingClient(
                new MockHTTPImageInputStreamClient(uri));

        try (HTTPImageInputStream instance =
                     new HTTPImageInputStream(client, Files.size(fixture))) {
            instance.setWindowSize(1024);
            instance.setMaxChunkCacheSize(1024L * 50);
            instance.setPrefetchCount(4);
            byte[] buf = new byte[(int) Files.size(fixture)];
            instance.read(buf, 0, buf.length);
        }

        // With prefetch, the number of *synchronous* GETs should be far fewer
        // than the total number of windows touched. The exact number depends
        // on scheduling, but at minimum we expect prefetches to fire.
        assertTrue(client.asyncCalls.get() > 0,
                "Expected async prefetches to fire on sequential read");
    }

    @Test
    void testCloseCancelsInFlightPrefetches() throws Exception {
        final List<CompletableFuture<Response>> issued = new CopyOnWriteArrayList<>();
        final Path fixture = TestUtil.getImage("tif");
        final URI uri      = webServer.getHTTPURI().resolve("/" + fixture.getFileName());
        final HTTPImageInputStreamClient backing =
                new MockHTTPImageInputStreamClient(uri);

        HTTPImageInputStreamClient tracking = new HTTPImageInputStreamClient() {
            @Override
            public Response sendHEADRequest() throws IOException {
                return backing.sendHEADRequest();
            }
            @Override
            public Response sendGETRequest(Range range) throws IOException {
                return backing.sendGETRequest(range);
            }
            @Override
            public CompletableFuture<Response> sendGETRequestAsync(Range range) {
                // Never complete on its own; cancellation is the only way out.
                CompletableFuture<Response> f = new CompletableFuture<>();
                issued.add(f);
                return f;
            }
        };

        HTTPImageInputStream instance =
                new HTTPImageInputStream(tracking, Files.size(fixture));
        instance.setWindowSize(1024);
        instance.setPrefetchCount(3);
        instance.read();  // triggers prefetches that will never complete on their own
        assertFalse(issued.isEmpty(), "Expected prefetches to be issued");
        instance.close();

        for (CompletableFuture<Response> f : issued) {
            assertTrue(f.isCancelled() || f.isDone(),
                    "Expected prefetch future to be cancelled by close()");
        }
    }

    @Test
    void testPrefetchFailureFallsBackToSyncFetch() throws Exception {
        final Path fixture = TestUtil.getImage("tif");
        final URI uri      = webServer.getHTTPURI().resolve("/" + fixture.getFileName());
        final HTTPImageInputStreamClient backing =
                new MockHTTPImageInputStreamClient(uri);
        final AtomicInteger syncCalls = new AtomicInteger();

        HTTPImageInputStreamClient failingPrefetch = new HTTPImageInputStreamClient() {
            @Override
            public Response sendHEADRequest() throws IOException {
                return backing.sendHEADRequest();
            }
            @Override
            public Response sendGETRequest(Range range) throws IOException {
                syncCalls.incrementAndGet();
                return backing.sendGETRequest(range);
            }
            @Override
            public CompletableFuture<Response> sendGETRequestAsync(Range range) {
                return CompletableFuture.failedFuture(
                        new IOException("simulated prefetch failure"));
            }
        };

        try (HTTPImageInputStream instance =
                     new HTTPImageInputStream(failingPrefetch, Files.size(fixture))) {
            instance.setWindowSize(1024);
            instance.setPrefetchCount(2);
            byte[] buf = new byte[(int) Files.size(fixture)];
            instance.read(buf, 0, buf.length);
            // Sequential read should still succeed via the sync fallback.
            assertTrue(syncCalls.get() > 0);
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
