package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.MockFileProcessor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InfoServiceTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private InfoService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);

        InfoService.clearInstance();
        instance = InfoService.getInstance();
    }

    private FileProcessor newFileProcessor() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY,
                "ManualSelectionStrategy");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
        try (FileProcessor proc = (FileProcessor) new ProcessorFactory().
                newProcessor(Format.JPG)) {
            proc.setSourceFormat(Format.JPG);
            proc.setSourceFile(TestUtil.getImage("jpg"));
            return proc;
        }
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
    void testGetInfoWithHitInMemoryCache() throws Exception {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();
        instance.putInObjectCache(identifier, info);

        Optional<Info> actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    @Test
    void testGetInfoWithHitInDerivativeCache() throws Exception {
        useFilesystemCache();

        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(identifier, info);

        Optional<Info> actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    @Test
    void testGetInfoWithMissEverywhere() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Optional<Info> info = instance.getInfo(identifier);
        assertFalse(info.isPresent());
    }

    /* getOrReadInfo() */

    @Test
    void testGetOrReadInfoWithHitInMemoryCache() throws Exception {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();
        instance.putInObjectCache(identifier, info);

        Optional<Info> actualInfo = instance.getOrReadInfo(identifier, newMockProcessor());
        assertEquals(info, actualInfo.orElse(null));
    }

    @Test
    void testGetOrReadInfoWithHitInDerivativeCache() throws Exception {
        useFilesystemCache();

        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(identifier, info);

        Optional<Info> actualInfo = instance.getOrReadInfo(identifier, newMockProcessor());
        assertEquals(info, actualInfo.orElse(null));
    }

    @Test
    void testGetOrReadInfoWithHitInProcessor() throws Exception {
        final Identifier identifier = new Identifier("jpg");

        Optional<Info> info = instance.getOrReadInfo(identifier, newFileProcessor());
        assertEquals(identifier, info.orElseThrow().getIdentifier());
        assertEquals(64, info.orElseThrow().getSize(0).width(), DELTA);
    }

    /* isObjectCacheEnabled() */

    @Test
    void testIsObjectCacheEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.INFO_CACHE_ENABLED, true);
        assertTrue(instance.isObjectCacheEnabled());

        config.setProperty(Key.INFO_CACHE_ENABLED, false);
        assertFalse(instance.isObjectCacheEnabled());
    }

    /* purgeObjectCache() */

    @Test
    void testPurgeObjectCache() {
        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.putInObjectCache(identifier, info);
        assertEquals(1, instance.getInfoCache().size());

        instance.purgeObjectCache();
        assertEquals(0, instance.getInfoCache().size());
    }

    /* purgeObjectCache(Identifier) */

    @Test
    void testPurgeObjectCacheWithIdentifier() {
        final Identifier id1 = new Identifier("cats");
        final Identifier id2 = new Identifier("dogs");
        final Info info = new Info();
        instance.putInObjectCache(id1, info);
        instance.putInObjectCache(id2, info);
        assertEquals(2, instance.getInfoCache().size());

        instance.purgeObjectCache(id1);
        assertEquals(1, instance.getInfoCache().size());
    }

}
