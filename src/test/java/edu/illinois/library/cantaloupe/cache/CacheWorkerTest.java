package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CacheWorkerTest extends BaseTest {

    private static final int TTL = 2;

    private CacheWorker instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Set up FilesystemCache as a source and derivative cache.
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                Files.createTempDirectory("test"));

        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, TTL);
        config.setProperty(Key.DERIVATIVE_CACHE,
                FilesystemCache.class.getSimpleName());

        config.setProperty(Key.SOURCE_CACHE_TTL, TTL);
        config.setProperty(Key.SOURCE_CACHE,
                FilesystemCache.class.getSimpleName());

        instance = new CacheWorker(-1); // we aren't using the interval
    }

    @Disabled // this is currently too hard to test
    @Test
    void testRunCleansUpContent() {
    }

    @Test
    void testRunPurgesInvalidDerivativeContent() throws Exception {
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        Identifier identifier = new Identifier("cats");
        cache.put(identifier, new Info());

        assertNotNull(cache.getInfo(identifier));

        Thread.sleep(TTL * 1000 * 2);

        instance.run();

        assertFalse(cache.getInfo(identifier).isPresent());
    }

    @Test
    void testRunPurgesInvalidSourceContent() throws Exception {
        SourceCache cache = CacheFactory.getSourceCache();
        Identifier identifier = new Identifier("cats");

        try (OutputStream os = cache.newSourceImageOutputStream(identifier)) {
            Files.copy(TestUtil.getImage("jpg"), os);
        }

        assertNotNull(cache.getSourceImageFile(identifier));

        Thread.sleep(TTL * 1000 * 2);

        instance.run();

        assertFalse(cache.getSourceImageFile(identifier).isPresent());
    }

    @Test
    void testRunDumpsHeapCache() throws Exception {
        Path dir = Files.createTempDirectory("test");
        Path file = dir.resolve("dump");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE, HeapCache.class.getSimpleName());
        config.setProperty(Key.HEAPCACHE_PERSIST, true);
        config.setProperty(Key.HEAPCACHE_PATHNAME, file);

        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(new Identifier("cats"), new Info());

        assertFalse(Files.exists(file));

        instance.run();

        assertTrue(Files.exists(file));
    }

}
