package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import io.findify.s3mock.S3Mock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class S3CacheTest extends AbstractCacheTest {

    private static S3Mock mockS3;
    private static int mockS3Port;

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private OperationList opList = new OperationList();
    private S3Cache instance;

    @BeforeClass
    public static void beforeClass() {
        startServiceIfNecessary();
        createBucket();
    }

    @AfterClass
    public static void afterClass() {
        if (mockS3 != null) {
            mockS3.stop();
        }
    }

    private static AmazonS3 client() {
        return new AWSClientBuilder()
                .endpointURI(getEndpoint())
                .accessKeyID(getAccessKeyId())
                .secretKey(getSecretKey())
                .build();
    }

    private static void createBucket() {
        final AmazonS3 s3 = client();
        final String bucketName = getBucket();

        try {
            s3.deleteBucket(bucketName);
        } catch (AmazonS3Exception e) {
            // This probably means it already exists. We'll find out shortly.
        }
        try {
            s3.createBucket(new CreateBucketRequest(bucketName));
        } catch (AmazonS3Exception e) {
            if (!e.getErrorCode().startsWith("BucketAlreadyOwnedByYou")) {
                throw e;
            }
        }
    }

    /**
     * Starts a mock S3 service if {@link #getEndpoint()} returns a localhost
     * URI.
     */
    private static void startServiceIfNecessary() {
        if ("localhost".equals(getEndpoint().getHost())) {
            mockS3Port = SocketUtils.getOpenPort();
            mockS3 = new S3Mock.Builder()
                    .withPort(mockS3Port)
                    .withInMemoryBackend()
                    .build();
            mockS3.start();
        }
    }

    private static String getAccessKeyId() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ACCESS_KEY_ID.getKey());
    }

    private static String getBucket() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_BUCKET.getKey());
    }

    private static URI getEndpoint() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        String endpointStr = testConfig.getString(ConfigurationConstants.S3_ENDPOINT.getKey());
        if (endpointStr == null || endpointStr.isEmpty()) {
            endpointStr = "http://localhost:" + mockS3Port;
        }
        try {
            return new URI(endpointStr);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = newInstance();
        instance.initialize();
    }

    @After
    public void tearDown() throws Exception {
        instance.purge();
        instance.shutdown();
    }

    @Override
    S3Cache newInstance() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3CACHE_OBJECT_KEY_PREFIX, "test/");
        config.setProperty(Key.S3CACHE_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.S3CACHE_BUCKET_NAME, getBucket());
        config.setProperty(Key.S3CACHE_SECRET_KEY, getSecretKey());
        config.setProperty(Key.S3CACHE_ENDPOINT, getEndpoint().toString());

        return new S3Cache();
    }

    /* getBucketName() */

    @Test
    public void testGetBucketName() {
        assertEquals(
                Configuration.getInstance().getString(Key.S3CACHE_BUCKET_NAME),
                instance.getBucketName());
    }

    /* getImageInfo(Identifier) */

    @Ignore // TODO: s3mock doesn't like this
    @Test
    @Override
    public void testGetImageInfoWithExistingInvalidImage() {}

    /* getObjectKey(Identifier) */

    @Test
    public void testGetObjectKeyWithIdentifier() {
        assertEquals(
                instance.getObjectKeyPrefix() + "info/" + identifier.toString() + ".json",
                instance.getObjectKey(identifier));
    }

    /* getObjectKey(OperationList */

    @Test
    public void testGetObjectKeyWithOperationList() {
        assertEquals(
                instance.getObjectKeyPrefix() + "image/" + opList.toString(),
                instance.getObjectKey(opList));
    }

    /* getObjectKeyPrefix() */

    @Test
    public void testGetObjectKeyPrefix() {
        Configuration config = Configuration.getInstance();

        config.setProperty(Key.S3CACHE_OBJECT_KEY_PREFIX, "");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.S3CACHE_OBJECT_KEY_PREFIX, "/");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.S3CACHE_OBJECT_KEY_PREFIX, "cats");
        assertEquals("cats/", instance.getObjectKeyPrefix());

        config.setProperty(Key.S3CACHE_OBJECT_KEY_PREFIX, "cats/");
        assertEquals("cats/", instance.getObjectKeyPrefix());
    }

    @Ignore // TODO: s3mock doesn't like this
    @Test
    @Override
    public void testNewDerivativeImageInputStreamWithNonzeroTTL() {}

}
