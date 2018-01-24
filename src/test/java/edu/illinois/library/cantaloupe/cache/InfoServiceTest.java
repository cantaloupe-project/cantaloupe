package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.MockFileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class InfoServiceTest extends BaseTest {

    private InfoService instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);

        InfoService.clearInstance();
        instance = InfoService.getInstance();
    }

    private FileProcessor newFileProcessor() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
        FileProcessor proc = (FileProcessor) new ProcessorFactory().
                newProcessor(Format.JPG);
        proc.setSourceFormat(Format.JPG);
        proc.setSourceFile(TestUtil.getImage("jpg"));
        return proc;
    }

    private FileProcessor newMockProcessor() throws Exception {
        FileProcessor proc = new MockFileProcessor();
        proc.setSourceFormat(Format.JPG);
        proc.setSourceFile(TestUtil.getImage("jpg"));
        return proc;
    }

    private void useFilesystemCache() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    Files.createTempDirectory("test").toString());
            config.setProperty(Key.DERIVATIVE_CACHE_TTL, "10");
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /* getInfo() */

    @Test
    public void testGetInfoWithHitInMemoryCache() throws Exception {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info(500, 300);
        instance.putInObjectCache(identifier, info);

        Info actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo);
    }

    @Test
    public void testGetInfoWithHitInDerivativeCache() throws Exception {
        useFilesystemCache();

        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info(500, 300);

        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(identifier, info);

        Info actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo);
    }

    @Test
    public void testGetInfoWithMissEverywhere() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Info info = instance.getInfo(identifier);
        assertNull(info);
    }

    /* getOrReadInfo() */

    @Test
    public void testGetOrReadInfoWithHitInMemoryCache() throws Exception {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info(500, 300);
        instance.putInObjectCache(identifier, info);

        Info actualInfo = instance.getOrReadInfo(identifier, newMockProcessor());
        assertEquals(info, actualInfo);
    }

    @Test
    public void testGetOrReadInfoWithHitInDerivativeCache() throws Exception {
        useFilesystemCache();

        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info(500, 300);

        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(identifier, info);

        Info actualInfo = instance.getOrReadInfo(identifier, newMockProcessor());
        assertEquals(info, actualInfo);
    }

    @Test
    public void testGetOrReadInfoWithHitInProcessor() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Info info = instance.getOrReadInfo(identifier, newFileProcessor());
        assertEquals(64, info.getSize(0).width);
    }

    /**
     * This should never happen in normal use because
     * {@link Processor#readImageInfo()} should never return <code>null</code>.
     */
    @Test
    public void testGetOrReadInfoWithMissEverywhere() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Info info = instance.getOrReadInfo(identifier, newMockProcessor());
        assertTrue(info.getImages().isEmpty());
    }

    /* isObjectCacheEnabled() */

    @Test
    public void testIsObjectCacheEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        assertTrue(instance.isObjectCacheEnabled());

        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        assertFalse(instance.isObjectCacheEnabled());
    }

    /* purgeObjectCache() */

    @Test
    public void testPurgeObjectCache() {
        final Identifier identifier = new Identifier("cats");
        final Info info = new Info(500, 300);
        instance.putInObjectCache(identifier, info);
        assertEquals(1, instance.getInfoCache().size());

        instance.purgeObjectCache();
        assertEquals(0, instance.getInfoCache().size());
    }

    /* purgeObjectCache(Identifier) */

    @Test
    public void testPurgeObjectCacheWithIdentifier() {
        final Identifier id1 = new Identifier("cats");
        final Identifier id2 = new Identifier("dogs");
        final Info info = new Info(500, 300);
        instance.putInObjectCache(id1, info);
        instance.putInObjectCache(id2, info);
        assertEquals(2, instance.getInfoCache().size());

        instance.purgeObjectCache(id1);
        assertEquals(1, instance.getInfoCache().size());
    }

}
