package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class CacheFacadeTest extends BaseTest {

    private CacheFacade instance;

    @Before
    public void setUp() {
        instance = new CacheFacade();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
    }

    private void enableDerivativeCache() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    Files.createTempDirectory("test").toString());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private void disableDerivativeCache() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, false);
    }

    private void enableInfoCache() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
    }

    private void disableInfoCache() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, false);
    }

    /* getDerivativeCache() */

    @Test
    public void testGetDerivativeCacheWhenEnabled() {
        enableDerivativeCache();
        assertNotNull(instance.getDerivativeCache());
    }

    @Test
    public void testGetDerivativeCacheWhenDisabled() {
        disableDerivativeCache();
        assertNull(instance.getDerivativeCache());
    }

    /* getInfo() */

    @Test
    public void testGetInfo() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Info expected = InfoService.getInstance().getInfo(identifier);
        Info actual = instance.getInfo(identifier);
        assertEquals(expected, actual);
    }

    /* getOrReadInfo() */

    @Test
    public void testGetOrReadInfo() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY,
                "ManualSelectionStrategy");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        final Identifier identifier = new Identifier("jpg");
        try (FileProcessor processor = (FileProcessor) new ProcessorFactory().
                newProcessor(Format.JPG)) {
            processor.setSourceFormat(Format.JPG);
            processor.setSourceFile(TestUtil.getImage(identifier.toString()));

            Info expected = InfoService.getInstance().getOrReadInfo(identifier, processor);
            Info actual = instance.getOrReadInfo(identifier, processor);
            assertEquals(expected, actual);
        }
    }

    /* getSourceCache() */

    @Test
    public void testGetSourceCache() {
        assertNotNull(instance.getSourceCache());
    }

    /* getSourceCacheFile() */

    @Test
    public void testGetSourceCacheFileWithSourceCacheHit() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");

        SourceCache sourceCache = CacheFactory.getSourceCache();
        Identifier identifier = new Identifier("cats");
        Path image = TestUtil.getImage("jpg");

        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }

        assertNotNull(instance.getSourceCacheFile(identifier));
    }

    @Test
    public void testGetSourceCacheFileWithSourceCacheMiss() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");

        Identifier identifier = new Identifier("cats");

        assertNull(instance.getSourceCacheFile(identifier));
    }

    @Test
    public void testGetSourceCacheFileWithInvalidSourceCache()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, "BogusCache");

        Identifier identifier = new Identifier("cats");

        instance.getSourceCacheFile(identifier);
    }

    /* isDerivativeCacheAvailable() */

    @Test
    public void testIsDerivativeCacheAvailable() {
        enableDerivativeCache();
        assertTrue(instance.isDerivativeCacheAvailable());

        disableDerivativeCache();
        assertFalse(instance.isDerivativeCacheAvailable());
    }

    /* isInfoCacheAvailable() */

    @Test
    public void testIsInfoCacheAvailable() {
        enableInfoCache();
        assertTrue(instance.isInfoCacheAvailable());

        disableInfoCache();
        assertFalse(instance.isInfoCacheAvailable());
    }

    /* newDerivativeImageInputStream() */

    @Test
    public void testNewDerivativeImageInputStreamWhenDerivativeCacheIsEnabled()
            throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"));

        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }
    }

    @Test
    public void testNewDerivativeImageInputStreamWhenDerivativeCacheIsDisabled()
            throws Exception {
        disableDerivativeCache();
        OperationList opList = new OperationList();
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* newDerivativeImageOutputStream() */

    @Test
    public void testNewDerivativeImageOutputStreamWhenDerivativeCacheIsEnabled()
            throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"));
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            assertNotNull(os);
        }
    }

    @Test
    public void testNewDerivativeImageOutputStreamWhenDerivativeCacheIsDisabled()
            throws Exception {
        disableDerivativeCache();
        OperationList opList = new OperationList();
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            assertNull(os);
        }
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        enableDerivativeCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }

        instance.purge();

        // Assert that everything is gone.
        assertEquals(0, InfoService.getInstance().getInfoCache().size());
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        enableDerivativeCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }

        instance.purge(identifier);

        // Assert that everything is gone.
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    /* purgeAsync(Identifier) */

    @Test
    public void testPurgeAsyncWithIdentifier() throws Exception {
        enableDerivativeCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }

        instance.purgeAsync(identifier);

        Thread.sleep(2000);

        // Assert that everything is gone.
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("cats"));

        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.purge(opList);

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* purgeInvalid() */

    @Test
    public void testPurgeExpired() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_TTL, 1);
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        enableDerivativeCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }

        instance.purgeExpired();

        Thread.sleep(1001);

        // Assert that everything is gone.
        assertEquals(0, InfoService.getInstance().getInfoCache().size());
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

}
