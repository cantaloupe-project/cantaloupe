package edu.illinois.library.cantaloupe.source;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.S3Utils;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class S3StreamFactoryTest extends BaseTest {

    private static final String FIXTURE_KEY = "jpg";

    private S3StreamFactory instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3Utils.createBucket(client(), bucket());
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        S3Utils.deleteBucket(client(), bucket());
    }

    private static String accessKeyID() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ACCESS_KEY_ID.getKey());
    }

    private static String bucket() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_BUCKET.getKey());
    }

    private static String endpoint() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ENDPOINT.getKey());
    }

    private static String secretAccessKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    private static AmazonS3 client() {
        return new AWSClientBuilder()
                .endpointURI(URI.create(endpoint()))
                .accessKeyID(accessKeyID())
                .secretKey(secretAccessKey())
                .build();
    }

    private static void configureS3Source() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_ENDPOINT, endpoint());
        config.setProperty(Key.S3SOURCE_ACCESS_KEY_ID, accessKeyID());
        config.setProperty(Key.S3SOURCE_SECRET_KEY, secretAccessKey());
    }

    private static void seedFixtures() {
        final AmazonS3 s3  = client();
        final Path fixture = TestUtil.getImage(FIXTURE_KEY);
        s3.putObject(new PutObjectRequest(
                bucket(), fixture.getFileName().toString(),
                fixture.toFile()));
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configureS3Source();
        seedFixtures();

        S3ObjectInfo info = new S3ObjectInfo(FIXTURE_KEY, bucket());
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
