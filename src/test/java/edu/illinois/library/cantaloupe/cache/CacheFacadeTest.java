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
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

public class CacheFacadeTest extends BaseTest {

    private CacheFacade instance;

    @Before
    public void setUp() {
        instance = new CacheFacade();
    }

    private void enableDerivativeCache() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    TestUtil.getTempFolder().getAbsolutePath());
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

    private void enableSourceCache() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
        config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
    }

    private void disableSourceCache() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_ENABLED, false);
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
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        final Identifier identifier = new Identifier("jpg");
        final FileProcessor processor = (FileProcessor) new ProcessorFactory().
                newProcessor(Format.JPG);
        processor.setSourceFormat(Format.JPG);
        processor.setSourceFile(TestUtil.getImage(identifier.toString()));

        Info expected = InfoService.getInstance().getOrReadInfo(identifier, processor);
        Info actual = instance.getOrReadInfo(identifier, processor);
        assertEquals(expected, actual);
    }

    /* getSourceCache() */

    @Test
    public void testGetSourceCacheWhenEnabled() {
        enableSourceCache();
        assertNotNull(instance.getSourceCache());
    }

    @Test
    public void testGetSourceCacheWhenDisabled() {
        disableSourceCache();
        assertNull(instance.getSourceCache());
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

    /* isSourceCacheAvailable() */

    @Test
    public void testIsSourceCacheAvailable() {
        enableSourceCache();
        assertTrue(instance.isSourceCacheAvailable());

        disableSourceCache();
        assertFalse(instance.isSourceCacheAvailable());
    }

    /* newDerivativeImageInputStream() */

    @Test
    public void testNewDerivativeImageInputStreamWhenDerivativeCacheIsEnabled()
            throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"),
                Format.JPG);

        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            IOUtils.copy(is, os);
        }

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }
    }

    @Test
    public void testNewDerivativeImageInputStreamWhenDerivativeCacheIsDisabled()
            throws Exception {
        disableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"),
                Format.JPG);
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* newDerivativeImageOutputStream() */

    @Test
    public void testNewDerivativeImageOutputStreamWhenDerivativeCacheIsEnabled()
            throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"),
                Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            assertNotNull(os);
        }
    }

    @Test
    public void testNewDerivativeImageOutputStreamWhenDerivativeCacheIsDisabled()
            throws Exception {
        disableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"),
                Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            assertNull(os);
        }
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        enableDerivativeCache();
        enableSourceCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList opList = new OperationList(identifier, Format.JPG);
        Info info = new Info();

        // Add identifier to the source cache.
        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            IOUtils.copy(is, os);
        }

        // Add opList to the derivative cache.
        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            IOUtils.copy(is, os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getImageInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.purge();

        // Assert that everything is gone.
        assertEquals(0, InfoService.getInstance().getObjectCacheSize());
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertNull(derivCache.getImageInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        enableDerivativeCache();
        enableSourceCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList opList = new OperationList(identifier, Format.JPG);
        Info info = new Info();

        // Add identifier to the source cache.
        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            IOUtils.copy(is, os);
        }

        // Add opList to the derivative cache.
        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            IOUtils.copy(is, os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getImageInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.purge(identifier);

        // Assert that everything is gone.
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertNull(derivCache.getImageInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* purgeAsync(Identifier) */

    @Test
    public void testPurgeAsyncWithIdentifier() {
        // TODO: write this
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"),
                Format.JPG);

        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            IOUtils.copy(is, os);
        }

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.purge(opList);

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        enableDerivativeCache();
        enableSourceCache();
        SourceCache sourceCache = CacheFactory.getSourceCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache();

        Identifier identifier = new Identifier("jpg");
        OperationList opList = new OperationList(identifier, Format.JPG);
        Info info = new Info();

        // Add identifier to the source cache.
        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            IOUtils.copy(is, os);
        }

        // Add opList to the derivative cache.
        try (InputStream is = new FileInputStream(TestUtil.getImage("jpg"));
             OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            IOUtils.copy(is, os);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getImageInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.purgeExpired();

        Thread.sleep(1001);

        // Assert that everything is gone.
        assertEquals(0, InfoService.getInstance().getObjectCacheSize());
        assertNull(sourceCache.getSourceImageFile(identifier));
        assertNull(derivCache.getImageInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

}
