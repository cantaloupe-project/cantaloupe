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
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CacheFacadeTest extends BaseTest {

    private static final int ASYNC_WAIT = 2000;

    private CacheFacade instance;

    @BeforeEach
    public void setUp() {
        instance = new CacheFacade();
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_CACHE, FilesystemCache.class.getSimpleName());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    Files.createTempDirectory("test").toString());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private void enableDerivativeCache() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, FilesystemCache.class.getSimpleName());
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
    void testGetDerivativeCacheWhenEnabled() {
        enableDerivativeCache();
        assertTrue(instance.getDerivativeCache().isPresent());
    }

    @Test
    void testGetDerivativeCacheWhenDisabled() {
        disableDerivativeCache();
        assertFalse(instance.getDerivativeCache().isPresent());
    }

    /* getInfo() */

    @Test
    void testGetInfo() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Optional<Info> expected = InfoService.getInstance().getInfo(identifier);
        Optional<Info> actual = instance.getInfo(identifier);
        assertEquals(expected, actual);
    }

    /* getOrReadInfo() */

    @Test
    void testGetOrReadInfo() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY,
                "ManualSelectionStrategy");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        final Identifier identifier = new Identifier("jpg");
        try (FileProcessor processor = (FileProcessor) new ProcessorFactory().
                newProcessor(Format.get("jpg"))) {
            processor.setSourceFormat(Format.get("jpg"));
            processor.setSourceFile(TestUtil.getImage(identifier.toString()));

            Optional<Info> expected = InfoService.getInstance().getOrReadInfo(identifier, processor);
            Optional<Info> actual = instance.getOrReadInfo(identifier, processor);
            assertEquals(expected, actual);
        }
    }

    /* getSourceCache() */

    @Test
    void testGetSourceCache() {
        assertTrue(instance.getSourceCache().isPresent());
    }

    /* getSourceCacheFile() */

    @Test
    void testGetSourceCacheFileWithSourceCacheHit() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, FilesystemCache.class.getSimpleName());

        SourceCache sourceCache = CacheFactory.getSourceCache().get();
        Identifier identifier = new Identifier("cats");
        Path image = TestUtil.getImage("jpg");

        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }

        assertNotNull(instance.getSourceCacheFile(identifier));
    }

    @Test
    void testGetSourceCacheFileWithSourceCacheMiss() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, FilesystemCache.class.getSimpleName());

        Identifier identifier = new Identifier("cats");

        assertFalse(instance.getSourceCacheFile(identifier).isPresent());
    }

    @Test
    void testGetSourceCacheFileWithInvalidSourceCache()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, "BogusCache");

        Identifier identifier = new Identifier("cats");

        instance.getSourceCacheFile(identifier);
    }

    /* isDerivativeCacheAvailable() */

    @Test
    void testIsDerivativeCacheAvailable() {
        enableDerivativeCache();
        assertTrue(instance.isDerivativeCacheAvailable());

        disableDerivativeCache();
        assertFalse(instance.isDerivativeCacheAvailable());
    }

    /* isInfoCacheAvailable() */

    @Test
    void testIsInfoCacheAvailable() {
        enableInfoCache();
        assertTrue(instance.isInfoCacheAvailable());

        disableInfoCache();
        assertFalse(instance.isInfoCacheAvailable());
    }

    /* newDerivativeImageInputStream() */

    @Test
    void testNewDerivativeImageInputStreamWhenDerivativeCacheIsEnabled()
            throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"));

        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(TestUtil.getImage("jpg"), os);
            os.setComplete(true);
        }

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }
    }

    @Test
    void testNewDerivativeImageInputStreamWhenDerivativeCacheIsDisabled()
            throws Exception {
        disableDerivativeCache();
        OperationList opList = new OperationList();
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* newDerivativeImageOutputStream() */

    @Test
    void testNewDerivativeImageOutputStreamWhenDerivativeCacheIsEnabled()
            throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("jpg"));
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList)) {
            assertNotNull(os);
            os.setComplete(true);
        }
    }

    @Test
    void testNewDerivativeImageOutputStreamWhenDerivativeCacheIsDisabled()
            throws Exception {
        disableDerivativeCache();
        OperationList opList = new OperationList();
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList)) {
            assertNull(os);
        }
    }

    /* purge() */

    @Test
    void testPurge() throws Exception {
        enableDerivativeCache();
        SourceCache sourceCache    = CacheFactory.getSourceCache().get();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache().get();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
            os.setComplete(true);
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
        assertFalse(sourceCache.getSourceImageFile(identifier).isPresent());
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    /* purge(Identifier) */

    @Test
    void testPurgeWithIdentifier() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: why does this fail in Windows?
        enableDerivativeCache();
        SourceCache sourceCache    = CacheFactory.getSourceCache().get();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache().get();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
            os.setComplete(true);
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
        assertFalse(sourceCache.getSourceImageFile(identifier).isPresent());
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    /* purgeAsync(Identifier) */

    @Test
    void testPurgeAsyncWithIdentifier() throws Exception {
        enableDerivativeCache();
        SourceCache sourceCache    = CacheFactory.getSourceCache().get();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache().get();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
            os.setComplete(true);
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

        Thread.sleep(ASYNC_WAIT);

        // Assert that everything is gone.
        assertFalse(sourceCache.getSourceImageFile(identifier).isPresent());
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    /* purge(OperationList) */

    @Test
    void testPurgeWithOperationList() throws Exception {
        enableDerivativeCache();
        OperationList opList = new OperationList(new Identifier("cats"));

        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(TestUtil.getImage("jpg"), os);
            os.setComplete(true);
        }

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        instance.purge(opList);

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* purgeInfos() */

    @Test
    void testPurgeInfos() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        enableDerivativeCache();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache().get();

        Identifier identifier = new Identifier("jpg");
        Info info             = new Info();

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(derivCache.getInfo(identifier));

        instance.purgeInfos();

        Thread.sleep(ASYNC_WAIT);

        // Assert that it's gone.
        assertEquals(0, InfoService.getInstance().getInfoCache().size());
        assertFalse(derivCache.getInfo(identifier).isPresent());
    }

    /* purgeInvalid() */

    @Test
    void testPurgeInvalid() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: this fails in Windows CI

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_TTL, 1);
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        enableDerivativeCache();
        SourceCache sourceCache    = CacheFactory.getSourceCache().get();
        DerivativeCache derivCache = CacheFactory.getDerivativeCache().get();

        Identifier identifier = new Identifier("jpg");
        OperationList ops = new OperationList(identifier);
        Info info = new Info();

        // Add identifier to the source cache.
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        // Add opList to the derivative cache.
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg"), os);
            os.setComplete(true);
        }

        // Add info to the derivative cache.
        derivCache.put(identifier, info);

        // Assert that everything has been added.
        assertNotNull(sourceCache.getSourceImageFile(identifier));
        assertNotNull(derivCache.getInfo(identifier));
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }

        instance.purgeInvalid();

        Thread.sleep(ASYNC_WAIT);

        // Assert that everything is gone.
        assertEquals(0, InfoService.getInstance().getInfoCache().size());
        assertFalse(sourceCache.getSourceImageFile(identifier).isPresent());
        assertFalse(derivCache.getInfo(identifier).isPresent());
        try (InputStream is = derivCache.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

}
