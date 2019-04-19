package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.TestUtil;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of tests shareable between major versions of IIIF Image
 * endpoints.
 */
public class ImageResourceTester extends ImageAPIResourceTester {

    public void testAuthorizationWhenRedirecting(URI uri)
            throws Exception {
        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertEquals(303, response.getStatus());
            assertEquals("http://example.org/",
                    response.getHeaders().getFirstValue("Location"));
        } finally {
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that a derivative and info have been added to the derivative
        // cache
        assertRecursiveFileCount(cacheDir, 2);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that a derivative and info have been added to the derivative
        // cache
        assertRecursiveFileCount(cacheDir, 2);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 200
            assertEquals(200, client.send().getStatus());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that a derivative and info have been added to the derivative
        // cache
        assertRecursiveFileCount(cacheDir, 2);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that a derivative and info have been added to the derivative
        // cache
        assertRecursiveFileCount(cacheDir, 2);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 200
            assertEquals(200, client.send().getStatus());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 0);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 0);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 0);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an image to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 0);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getInfoCache().size());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            Files.move(sourceFile, movedFile.toPath());

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            Files.move(movedFile.toPath(), sourceFile);
            client.stop();
        }
    }

    public void testContentDispositionHeaderWithNoHeader(URI uri)
            throws Exception {
        Client client = newClient(uri);
        try {
            Response response = client.send();
            assertNull(response.getHeaders().getFirstValue("Content-Disposition"));
        } finally {
            client.stop();
        }
    }

    public void testLessThanOrEqualToMaxScale(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);

        assertStatus(200, uri);
    }

    public void testGreaterThanMaxScale(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);

        assertStatus(403, uri);
    }

    public void testMinPixels(URI uri) {
        assertStatus(400, uri); // zero area
    }

    public void testLessThanMaxPixels(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_PIXELS, 100000000);

        assertStatus(200, uri);
    }

    public void testMoreThanMaxPixels(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_PIXELS, 1000);

        assertStatus(403, uri);
    }

    public void testMaxPixelsIgnoredWhenStreamingSource(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_PIXELS, 1000);

        assertStatus(200, uri);
    }

    public void testProcessorValidationFailure(URI uri) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY, "ManualSelectionStrategy");
        config.setProperty(Key.PROCESSOR_FALLBACK, "PdfBoxProcessor");

        assertStatus(400, uri);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(URI uri,
                                                                      OperationList opList)
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(uri, opList, false);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(URI uri,
                                                                     OperationList opList)
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(uri, opList, true);
    }

    private void doPurgeFromCacheWhenSourceIsMissing(URI uri,
                                                     OperationList opList,
                                                     boolean purgeMissing)
            throws Exception {
        // Create a directory that will contain a source image. We don't want
        // to use the image fixtures dir because we'll need to delete one.
        Path sourceDir = Files.createTempDirectory("source");

        // Populate the source directory with an image.
        Path imageFixture = TestUtil.getImage(IMAGE);
        Path sourceImage = sourceDir.resolve(imageFixture.getFileName());
        Files.copy(imageFixture, sourceImage);

        // Create the cache directory.
        Path cacheDir = Files.createTempDirectory("cache");

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                sourceDir.toString() + "/");
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME, cacheDir.toString());
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 60);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
        config.setProperty(Key.CACHE_SERVER_PURGE_MISSING, purgeMissing);

        Client client = newClient(uri);

        try {
            Info info = Info.builder().withSize(64, 56).build();
            opList.applyNonEndpointMutations(info, null);

            assertRecursiveFileCount(cacheDir, 0);

            // Request an image to cache it. This will cache both a derivative
            // and an info.
            client.send();

            // The info may write asynchronously, so wait.
            Thread.sleep(1000);

            // Assert that they've been cached.
            assertRecursiveFileCount(cacheDir, 2);

            // Delete the source image.
            Files.delete(sourceImage);

            // Request the same image which is now cached but underlying is
            // 404.
            try {
                client.send();
                fail("Expected exception");
            } catch (ResourceException e) {
                // good
            }

            // Stuff may be deleted asynchronously, so wait.
            Thread.sleep(1000);

            if (purgeMissing) {
                assertRecursiveFileCount(cacheDir, 0);
            } else {
                assertRecursiveFileCount(cacheDir, 2);
            }
        } finally {
            client.stop();
        }
    }

    public void testInvalidOutputFormat(URI uri) {
        assertStatus(415, uri);
    }

}
