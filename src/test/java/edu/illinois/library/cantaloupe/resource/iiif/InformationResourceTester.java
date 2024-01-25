package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.test.TestUtil;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of tests shareable between major versions of IIIF Information
 * endpoints.
 */
public class InformationResourceTester extends ImageAPIResourceTester {

    @Override
    public void testAuthorizationWhenUnauthorized(URI uri, String endpointPath) {
        final String requiredJsonLdContent;

        if (endpointPath.equals(Route.IIIF_1_PATH)) {
            requiredJsonLdContent = "\"@context\":\"http://library.stanford.edu/iiif/image-api/1.1/context.json\"";
        } else {
            requiredJsonLdContent = "\"protocol\":\"http://iiif.io/api/image\"";
        }

        assertStatus(401, uri);
        assertRepresentationContains(requiredJsonLdContent, uri);
    }

    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
            URI uri, Path sourceFile) throws Exception {
        final Path cacheDir = initializeFilesystemCache();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 1);

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

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 1);

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

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 1);

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

        // request an info to cache it
        Client client = newClient(uri);
        client.send();

        Thread.sleep(1000); // the info may write asynchronously

        // assert that an info has been added to the derivative cache
        assertRecursiveFileCount(cacheDir, 1);

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

        // request an info to cache it
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

        // request an info to cache it
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

            // request it again and assert HTTP 200
            assertEquals(200, client.send().getStatus());
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

        // request an info to cache it
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

        // request an info to cache it
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
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.toString());
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 60);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
        config.setProperty(Key.CACHE_SERVER_PURGE_MISSING, purgeMissing);

        Client client = newClient(uri);

        try {
            Identifier identifier = new Identifier(IMAGE);

            assertRecursiveFileCount(cacheDir, 0);

            // Request an image to cache its info.
            client.send();

            // The info may write asynchronously, so wait.
            Thread.sleep(1000);

            // Assert that it's been cached.
            assertRecursiveFileCount(cacheDir, 1);
            DerivativeCache cache = CacheFactory.getDerivativeCache().get();
            assertNotNull(cache.getInfo(identifier));

            // Delete the source image.
            Files.delete(sourceImage);

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
                assertFalse(cache.getInfo(identifier).isPresent());
            } else {
                assertTrue(cache.getInfo(identifier).isPresent());
            }
        } finally {
            client.stop();
        }
    }

    public void testRedirectToInfoJSON(URI fromURI, URI toURI) {
        assertRedirect(fromURI, toURI, 303);
    }

    public void testRedirectToInfoJSONWithEncodedCharacters(URI fromURI,
                                                            URI toURI) {
        assertRedirect(fromURI, toURI, 303);
    }

    public void testRedirectToInfoJSONWithDifferentPublicIdentifier(URI uri)
            throws Exception {
        Client client = newClient(uri);
        client.getHeaders().set(AbstractResource.PUBLIC_IDENTIFIER_HEADER, "foxes");
        try {
            Response response = client.send();
            assertEquals(303, response.getStatus());
            assertTrue(response.getHeaders().getFirstValue("Location").endsWith("/foxes/info.json"));
        } finally {
            client.stop();
        }
    }

}
