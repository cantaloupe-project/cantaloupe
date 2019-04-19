package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.S3Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class S3StreamFactoryTest extends BaseTest {

    private static final S3Server S3_SERVER = new S3Server();

    private S3StreamFactory instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3_SERVER.start();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        S3_SERVER.stop();
    }

    private static void configureS3Source() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_ENDPOINT, S3_SERVER.getEndpoint());
        config.setProperty(Key.S3SOURCE_ACCESS_KEY_ID, S3Server.ACCESS_KEY_ID);
        config.setProperty(Key.S3SOURCE_SECRET_KEY, S3Server.SECRET_KEY);
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configureS3Source();

        S3ObjectInfo info = new S3ObjectInfo("jpg", S3Server.FIXTURES_BUCKET_NAME);
        info.setLength(1584);

        instance = new S3StreamFactory(info);
    }

    @Test
    void testIsSeekingDirect() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_CHUNKING_ENABLED, false);
        assertFalse(instance.isSeekingDirect());
        config.setProperty(Key.S3SOURCE_CHUNKING_ENABLED, true);
        assertTrue(instance.isSeekingDirect());
    }

    @Test
    void testNewInputStream() throws Exception {
        int length = 0;
        try (InputStream is = instance.newInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(1584, length);
    }

    @Test
    void testNewSeekableStream() throws Exception {
        int length = 0;
        try (ImageInputStream is = instance.newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(1584, length);
    }

    @Test
    void testNewSeekableStreamClassWithChunkingEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.S3SOURCE_CHUNK_SIZE, "777K");

        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
            assertEquals(777 * 1024, ((HTTPImageInputStream) is).getWindowSize());
        }
    }

    @Test
    void testNewSeekableStreamClassWithChunkingDisabled() throws Exception {
        Configuration.getInstance().setProperty(Key.S3SOURCE_CHUNKING_ENABLED, false);
        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof ClosingMemoryCacheImageInputStream);
        }
    }

    @Test
    void testNewSeekableStreamWithChunkCacheEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.S3SOURCE_CHUNK_SIZE, "777K");
        config.setProperty(Key.S3SOURCE_CHUNK_CACHE_ENABLED, true);
        config.setProperty(Key.S3SOURCE_CHUNK_CACHE_MAX_SIZE, "5M");

        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
            HTTPImageInputStream htis = (HTTPImageInputStream) is;
            assertEquals(777 * 1024, htis.getWindowSize());
            assertEquals(Math.round((5 * 1024 * 1024) / (double) htis.getWindowSize()),
                    htis.getMaxChunkCacheSize());
        }
    }

}
