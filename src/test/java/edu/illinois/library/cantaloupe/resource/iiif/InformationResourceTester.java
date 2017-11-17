package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.api.ContentResponse;

import java.io.File;
import java.net.URI;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.*;
import static org.junit.Assert.*;

/**
 * Collection of tests shareable between major versions of IIIF Information
 * endpoints.
 */
public class InformationResourceTester extends ImageAPIResourceTester {

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 1);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 1);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 200
            assertEquals(200, client.send().getStatus());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 1);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 1);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 200
            assertEquals(200, client.send().getStatus());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 0);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 0);

        // assert that an info has been added to the info cache
        assertEquals(1, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 200
            assertEquals(200, client.send().getStatus());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 0);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
            URI uri, File sourceFile) throws Exception {
        final File cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that nothing has been added to the derivative cache
        assertRecursiveFileCount(cacheDir.toPath(), 0);

        // assert that an info has NOT been added to the info cache
        assertEquals(0, InfoService.getInstance().getObjectCacheSize());

        // move the source image out of the way
        File movedFile = new File(sourceFile + ".tmp");
        try {
            FileUtils.moveFile(sourceFile, movedFile);

            // request it again and assert HTTP 404
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        } finally {
            FileUtils.moveFile(movedFile, sourceFile);
            client.stop();
        }
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(URI uri)
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(uri, false);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(URI uri)
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(uri, true);
    }

    private void doPurgeFromCacheWhenSourceIsMissing(URI uri,
                                                     boolean purgeMissing)
            throws Exception {
        // Create a directory that will contain a source image. Don't want to
        // use the image fixtures dir because we'll need to delete one.
        File sourceDir = TestUtil.getTempFolder();
        sourceDir = new File(sourceDir.getAbsolutePath() + "/source");
        if (sourceDir.exists()) {
            FileUtils.cleanDirectory(sourceDir);
        } else {
            sourceDir.mkdir();
        }

        // Populate the source directory with an image.
        File imageFixture = TestUtil.getImage(IMAGE);
        File sourceImage = new File(sourceDir.getAbsolutePath() + "/" +
                imageFixture.getName());
        FileUtils.copyFile(imageFixture, sourceImage);

        // Create the cache directory.
        File cacheDir = TestUtil.getTempFolder();
        cacheDir = new File(cacheDir.getAbsolutePath() + "/cache");
        if (cacheDir.exists()) {
            FileUtils.cleanDirectory(cacheDir);
        } else {
            cacheDir.mkdir();
        }

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                sourceDir.getAbsolutePath() + "/");
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, 60);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
        config.setProperty(Key.CACHE_SERVER_PURGE_MISSING, purgeMissing);

        Client client = newClient(uri);

        try {
            Identifier identifier = new Identifier(IMAGE);

            assertRecursiveFileCount(cacheDir.toPath(), 0);

            // Request an image to cache its info.
            client.send();

            // The info may write asynchronously, so wait.
            Thread.sleep(1000);

            // Assert that it's been cached.
            assertRecursiveFileCount(cacheDir.toPath(), 1);
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            assertNotNull(cache.getImageInfo(identifier));

            // Delete the source image.
            assertTrue(sourceImage.delete());

            // Request the same image which is now cached but underlying is
            // gone.
            try {
                client.send();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(404, e.getStatusCode());
            }

            // Stuff may be deleted asynchronously, so wait.
            Thread.sleep(1000);

            if (purgeMissing) {
                assertNull(cache.getImageInfo(identifier));
            } else {
                assertNotNull(cache.getImageInfo(identifier));
            }
        } finally {
            client.stop();
            FileUtils.deleteDirectory(sourceDir);
            FileUtils.deleteDirectory(cacheDir);
        }
    }

    public void testRedirectToInfoJSON(URI fromURI, URI toURI) {
        assertRedirect(fromURI, toURI, 303);
    }

    public void testRedirectToInfoJSONWithDifferentPublicIdentifier(URI uri)
            throws Exception {
        Client client = newClient(uri);
        client.getHeaders().put(AbstractResource.PUBLIC_IDENTIFIER_HEADER, "foxes");
        try {
            ContentResponse response = client.send();
            assertEquals(303, response.getStatus());
            assertTrue(response.getHeaders().get("Location").endsWith("/foxes/info.json"));
        } finally {
            client.stop();
        }
    }

}
