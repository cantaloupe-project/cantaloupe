package edu.illinois.library.cantaloupe.source;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.S3Server;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class S3StreamFactoryTest extends BaseTest {

    private static final S3Server S3_SERVER = new S3Server();

    private S3StreamFactory instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3_SERVER.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        S3_SERVER.stop();
    }

    private static AmazonS3 client() {
        return new AWSClientBuilder()
                .endpointURI(S3_SERVER.getEndpoint())
                .accessKeyID(S3Server.ACCESS_KEY_ID)
                .secretKey(S3Server.SECRET_KEY)
                .build();
    }

    private static void configureS3Source() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_ENDPOINT, S3_SERVER.getEndpoint());
        config.setProperty(Key.S3SOURCE_ACCESS_KEY_ID, S3Server.ACCESS_KEY_ID);
        config.setProperty(Key.S3SOURCE_SECRET_KEY, S3Server.SECRET_KEY);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configureS3Source();

        S3ObjectInfo info = new S3ObjectInfo("jpg", S3Server.FIXTURES_BUCKET_NAME);
        info.setLength(5439);

        GetObjectRequest request = new GetObjectRequest(
                info.getBucketName(),
                info.getKey());
        S3Object object = client().getObject(request);

        instance = new S3StreamFactory(info, object);
    }

    @Test
    public void testNewInputStream() throws Exception {
        int length = 0;
        try (InputStream is = instance.newInputStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    public void testNewSeekableStream() throws Exception {
        int length = 0;
        try (ImageInputStream is = instance.newSeekableStream()) {
            while (is.read() != -1) {
                length++;
            }
        }
        assertEquals(5439, length);
    }

    @Test
    public void testNewSeekableStreamClassWithChunkingEnabled() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_CHUNKING_ENABLED, true);
        config.setProperty(Key.S3SOURCE_CHUNK_SIZE, "777K");

        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof HTTPImageInputStream);
            assertEquals(777 * 1024, ((HTTPImageInputStream) is).getWindowSize());
        }
    }

    @Test
    public void testNewSeekableStreamClassWithChunkingDisabled() throws Exception {
        Configuration.getInstance().setProperty(Key.S3SOURCE_CHUNKING_ENABLED, false);
        try (ImageInputStream is = instance.newSeekableStream()) {
            assertTrue(is instanceof ClosingMemoryCacheImageInputStream);
        }
    }

    @Test
    public void testNewSeekableStreamWithChunkCacheEnabled() throws Exception {
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
