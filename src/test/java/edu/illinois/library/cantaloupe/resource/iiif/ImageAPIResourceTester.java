package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeInputStreamCache;
import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeOutputStreamCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.http.Transport;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.AccessDeniedSource;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.TestUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of tests common across major versions of IIIF Image and
 * Information endpoints.
 */
public class ImageAPIResourceTester {

    static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    Client newClient(URI uri) {
        return new Client().builder().uri(uri).build();
    }

    public void testAuthorizationWhenAuthorized(URI uri) {
        assertStatus(200, uri);
    }

    public void testAuthorizationWhenNotAuthorized(URI uri) {
        // This may vary depending on the return value of a delegate method,
        // but the way the tests are set up, it's 403.
        assertStatus(403, uri);
        assertRepresentationContains("403 Forbidden", uri);
    }

    public void testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(URI uri)
            throws Exception {
        initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 10);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);

        // Request the resource to cache it.
        // This status code may vary depending on the return value of a
        // delegate method, but the way the tests are set up, it's 403.
        assertStatus(403, uri);

        Thread.sleep(1000); // the resource may write asynchronously

        // Request it again. We expect to receive the same response. Any
        // different response would indicate a logic error.
        assertStatus(403, uri);
    }

    public void testAuthorizationWhenScaleConstraining(URI uri)
            throws Exception {
        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertEquals(302, response.getStatus());
            assertEquals(uri.toString().replace("reduce.jpg", "reduce.jpg-1:2"),
                    response.getHeaders().getFirstValue("Location"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(URI uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try {
            Response response = client.send();

            String header = response.getHeaders().getFirstValue("Cache-Control");
            assertTrue(header.contains("max-age=1234"));
            assertTrue(header.contains("s-maxage=4567"));
            assertTrue(header.contains("public"));
            assertTrue(header.contains("no-transform"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(URI uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals("no-cache, must-revalidate",
                    e.getResponse().getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    /**
     * Tests that there is no {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} but a {@code cache=false} argument
     * is present in the URL query.
     */
    public void testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(URI uri)
            throws Exception {
        enableCacheControlHeaders();

        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertNull(response.getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    public void testCacheHeadersWhenClientCachingIsDisabled(URI uri)
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, false);

        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertNull(response.getHeaders().getFirstValue("Cache-Control"));
        } finally {
            client.stop();
        }
    }

    public void testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(URI uri)
            throws Exception {
        Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);

        // request an info
        Client client = newClient(uri);
        try {
            client.send();

            Thread.sleep(1000); // it may write asynchronously

            // assert that neither the image nor the info exists in the
            // derivative cache
            assertRecursiveFileCount(cacheDir, 0);

            // assert that the info does NOT exist in the info cache
            assertEquals(0, InfoService.getInstance().getInfoCache().size());
        } finally {
            client.stop();
        }
    }

    public void testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(URI uri)
            throws Exception {
        Path cacheDir = initializeFilesystemCache();

        // request an image
        Client client = newClient(uri);
        try {
            client.send();

            class FileTimeHolder {
                FileTime time;
            }
            final FileTimeHolder timeHolder = new FileTimeHolder();
            class FileCreationTimeChecker<T> extends SimpleFileVisitor<T> {
                @Override
                public FileVisitResult visitFile(T file,
                                                 BasicFileAttributes attrs) throws IOException {
                    BasicFileAttributes fattrs = Files.readAttributes(
                            (Path) file,
                            BasicFileAttributes.class);
                    timeHolder.time = fattrs.creationTime();
                    return FileVisitResult.CONTINUE;
                }
            }

            final FileCreationTimeChecker<Path> visitor =
                    new FileCreationTimeChecker<>();
            FileTime time1, time2;

            // check its last-modified time
            Files.walkFileTree(cacheDir, visitor);
            time1 = timeHolder.time;

            // FileTime only has 1-second precision so wait at least that long.
            Thread.sleep(2000);

            // request it again
            client.send();

            // check its last-modified time again
            Files.walkFileTree(cacheDir, visitor);
            time2 = timeHolder.time;

            // assert that the times have changed
            assertTrue(time2.compareTo(time1) > 0);
        } finally {
            client.stop();
        }
    }

    public void testForwardSlashInIdentifier(URI uri) {
        assertStatus(200, uri);
    }

    public void testBackslashInIdentifier(URI uri) {
        assertStatus(200, uri);
    }

    public void testHTTP2(URI uri) throws Exception {
        Client client = newClient(uri);
        try {
            client.setTransport(Transport.HTTP2_0);
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    public void testHTTPS1_1(URI uri) throws Exception {
        Client client = newClient(uri);
        try {
            client.setTrustAll(true);
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    public void testHTTPS2(URI uri) throws Exception {
        Client client = newClient(uri);
        try {
            client.setTransport(Transport.HTTP2_0);
            client.setTrustAll(true);
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    public void testForbidden(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC,
                AccessDeniedSource.class.getName());

        assertStatus(403, uri);
    }

    public void testNotFound(URI uri) {
        assertStatus(404, uri);
    }

    /**
     * Tests recovery from an exception thrown by
     * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache#newDerivativeImageInputStream}.
     */
    public void testRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException(URI uri)
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE,
                MockBrokenDerivativeInputStreamCache.class.getSimpleName());
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        client.send();
    }

    /**
     * Tests recovery from an exception thrown by
     * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache#newDerivativeImageInputStream}.
     */
    public void testRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException(URI uri)
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE,
                MockBrokenDerivativeOutputStreamCache.class.getSimpleName());
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        Client client = newClient(uri);
        client.send();
    }

    public void testRecoveryFromIncorrectSourceFormat(URI uri) throws Exception {
        Client client = newClient(uri);
        try {
            client.send(); // should throw an exception if anything goes wrong
        } finally {
            client.stop();
        }
    }

    /**
     * Used by {@link #testSourceCheckAccessNotCalledWithSourceCacheHit}.
     */
    public static class NotCheckingAccessSource implements Source {

        @Override
        public void checkAccess() throws IOException {
            throw new IOException("checkAccess called!");
        }

        @Override
        public Iterator<Format> getFormatIterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Format next() {
                    return Format.get("jpg");
                }
            };
        }

        @Override
        public Identifier getIdentifier() {
            return null;
        }

        @Override
        public StreamFactory newStreamFactory() {
            return new PathStreamFactory(TestUtil.getImage("jpg"));
        }

        @Override
        public void setIdentifier(Identifier identifier) {}

        @Override
        public void setDelegateProxy(DelegateProxy proxy) {}

    }

    public void testSourceCheckAccessNotCalledWithSourceCacheHit(Identifier identifier,
                                                                 URI uri) throws Exception {
        // Set up the environment to use the source cache, not resolve first,
        // and use a non-FileSource.
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
        config.setProperty(Key.SOURCE_STATIC,
                NotCheckingAccessSource.class.getName());
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
        config.setProperty(Key.SOURCE_CACHE_TTL, 10);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                Files.createTempDirectory("test").toString());
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        // Put an image in the source cache.
        Path image = TestUtil.getImage("jpg");
        SourceCache sourceCache = CacheFactory.getSourceCache().get();

        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }

        Client client = newClient(uri);
        try {
            client.send();
            // We are expecting NonCheckingAccessSource.checkAccess() to not
            // throw an exception, which would cause a 500 response.
        } finally {
            client.stop();
        }
    }

    /**
     * Used by {@link #testSourceGetFormatNotCalledWithSourceCacheHit(Identifier, URI)}.
     */
    public static class NotReadingSourceFormatSource implements Source {

        @Override
        public void checkAccess() {}

        @Override
        public Iterator<Format> getFormatIterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Format next() {
                    return Format.UNKNOWN;
                }
            };
        }

        @Override
        public Identifier getIdentifier() {
            return null;
        }

        @Override
        public StreamFactory newStreamFactory() {
            return new PathStreamFactory(TestUtil.getImage("jpg"));
        }

        @Override
        public void setIdentifier(Identifier identifier) {}

        @Override
        public void setDelegateProxy(DelegateProxy proxy) {}

    }

    public void testSourceGetFormatNotCalledWithSourceCacheHit(Identifier identifier,
                                                               URI uri) throws Exception {
        // Set up the environment to use the source cache, not resolve first,
        // and use a non-FileSource.
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
        config.setProperty(Key.SOURCE_STATIC,
                NotReadingSourceFormatSource.class.getName());
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
        config.setProperty(Key.SOURCE_CACHE_TTL, 10);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                Files.createTempDirectory("test").toString());
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        // Put an image in the source cache.
        Path image = TestUtil.getImage("jpg");
        SourceCache sourceCache = CacheFactory.getSourceCache().get();

        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }

        Client client = newClient(uri);
        try {
            client.send();
            // We are expecting NotReadingSourceFormatSource.getFormatIterator()
            // to not throw an exception, which would cause a 500 response.
        } finally {
            client.stop();
        }
    }

    /**
     * Tests that the server responds with HTTP 500 when a {@link Source} that
     * {@link Source#supportsFileAccess() doesn't support file access} is used
     * with a non-{@link
     * edu.illinois.library.cantaloupe.processor.StreamProcessor}.
     */
    public void testSourceProcessorCompatibility(URI uri,
                                                 String appServerHost,
                                                 int appServerPort) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, "HttpSource");
        config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                appServerHost + ":" + appServerPort + "/");
        config.setProperty("processor.jp2", "OpenJpegProcessor");

        assertStatus(500, uri);
    }

    /**
     * @param uri URI containing <code>CATS</code> as the slash substitute.
     */
    public void testSlashSubstitution(URI uri) {
        Configuration.getInstance().setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        assertStatus(200, uri);
    }

    public void testUnavailableSourceFormat(URI uri) {
        assertStatus(501, uri);
    }

    private void enableCacheControlHeaders() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "true");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");
    }

    Path initializeFilesystemCache() throws IOException {
        Path cacheDir = Files.createTempDirectory("test");

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME, cacheDir.toString());
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 10);

        assertRecursiveFileCount(cacheDir, 0);

        return cacheDir;
    }

}
