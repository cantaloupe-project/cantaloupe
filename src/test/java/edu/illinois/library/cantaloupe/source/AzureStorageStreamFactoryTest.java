package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import edu.illinois.library.cantaloupe.test.AzureStorageTestUtil;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class AzureStorageStreamFactoryTest extends BaseTest {

    private AzureStorageStreamFactory instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        AzureStorageTestUtil.uploadFixtures();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        AzureStorageTestUtil.deleteFixtures();
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        CloudBlockBlob blob = AzureStorageTestUtil.client()
                .getContainerReference(AzureStorageTestUtil.getContainer())
                .getBlockBlobReference("jpg");
        instance = new AzureStorageStreamFactory(blob);
    }

    @Test
    void testIsSeekingDirect() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, false);
        assertFalse(instance.isSeekingDirect());
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, true);
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
    void testNewSeekableStreamTypeWithChunkingEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNK_SIZE, "777K");

        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
            assertEquals(777 * 1024, ((HTTPImageInputStream) is).getWindowSize());
        }
    }

    @Test
    void testNewSeekableStreamLengthWithChunkingEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNK_SIZE, "1K");

        int length = 0;
        try (ImageInputStream is = instance.newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(1584, length);
    }

    @Test
    void testNewSeekableStreamTypeWithChunkingDisabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, false);

        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof ClosingMemoryCacheImageInputStream);
        }
    }

    @Test
    void testNewSeekableStreamLengthWithChunkingDisabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, false);

        int length = 0;
        try (ImageInputStream is = instance.newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(1584, length);
    }

    @Test
    void testNewSeekableStreamWithChunkCacheEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNK_SIZE, "777K");
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNK_CACHE_ENABLED, true);
        config.setProperty(Key.AZURESTORAGESOURCE_CHUNK_CACHE_MAX_SIZE, "5M");

        try (ImageInputStream is = instance.newSeekableStream()) {
            HTTPImageInputStream htis = (HTTPImageInputStream) is;
            assertEquals(777 * 1024, htis.getWindowSize());
            assertEquals(Math.round((5 * 1024 * 1024) / (double) htis.getWindowSize()),
                    htis.getMaxChunkCacheSize());
        }
    }

}
