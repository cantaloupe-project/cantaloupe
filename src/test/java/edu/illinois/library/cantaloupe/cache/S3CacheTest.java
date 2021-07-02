package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.S3ClientBuilder;
import edu.illinois.library.cantaloupe.util.S3Utils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class S3CacheTest extends AbstractCacheTest {

    private enum Service {
        AWS("aws"), MINIO("minio");

        private final String key;

        static Service forKey(String key) {
            return Arrays.stream(values())
                    .filter(s -> key.equals(s.key))
                    .findFirst()
                    .orElse(null);
        }

        Service(String key) {
            this.key = key;
        }
    }

    private static S3Client client;

    private final Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private final OperationList opList  = new OperationList();
    private S3Cache instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3Utils.createBucket(client(), getBucket());
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        if (client != null) {
            client.close();
        }
    }

    private static synchronized S3Client client() {
        if (client == null) {
            client = new S3ClientBuilder()
                    .endpointURI(getEndpoint())
                    .region(getRegion())
                    .accessKeyID(getAccessKeyId())
                    .secretAccessKey(getSecretKey())
                    .build();
        }
        return client;
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
        if (endpointStr != null && !endpointStr.isBlank()) {
            try {
                return new URI(endpointStr);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    private static String getRegion() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_REGION.getKey());
    }

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    private static Service getService() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return Service.forKey(testConfig.getString(ConfigurationConstants.S3_SERVICE.getKey()));
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = newInstance();
        instance.initialize();
    }

    @AfterEach
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
    void testGetBucketName() {
        assertEquals(
                Configuration.getInstance().getString(Key.S3CACHE_BUCKET_NAME),
                instance.getBucketName());
    }

    /* getInfo(Identifier) */

    @Test
    void testGetInfoUpdatesLastModifiedTime() throws Exception {
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        final DerivativeCache instance = newInstance();

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        for (int i = 0; i < 10; i++) {
            Thread.sleep(250);
            assertNotNull(instance.getInfo(identifier));
        }
    }

    /* getObjectKey(Identifier) */

    @Test
    void testGetObjectKeyWithIdentifier() {
        assertEquals(
                "test/info/083425bc68eece64753ec83a25f87230.json",
                instance.getObjectKey(identifier));
    }

    /* getObjectKey(OperationList */

    @Test
    void testGetObjectKeyWithOperationList() {
        opList.setIdentifier(new Identifier("cats"));
        assertEquals(
                "test/image/0832c1202da8d382318e329a7c133ea0/4520700b2323f4d1e65e1b2074f43d47",
                instance.getObjectKey(opList));
    }

    /* getObjectKeyPrefix() */

    @Test
    void testGetObjectKeyPrefix() {
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

    @Test
    @Override
    void testNewDerivativeImageInputStreamWithNonzeroTTL() throws Exception {
        assumeFalse(Service.AWS.equals(getService()));  // TODO: this test fails in AWS

        super.testNewDerivativeImageInputStreamWithNonzeroTTL();
    }

    @Test
    void testNewDerivativeImageInputStreamUpdatesLastModifiedTime()
            throws Exception {
        assumeFalse(Service.MINIO.equals(getService())); // this test fails in minio

        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 2);

        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Path fixture = TestUtil.getImage(IMAGE);

        // Add an image.
        // N.B.: This method may return before data is fully (or even
        // partially) written to the cache.
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(fixture, os);
            os.setComplete(true);
        }

        // Wait for it to finish, hopefully.
        Thread.sleep(2000);

        // Assert that it has been added.
        assertExists(instance, ops);

        Thread.sleep(1000);

        assertExists(instance, ops);

        Thread.sleep(1000);

        assertExists(instance, ops);
    }

    @Test
    @Override
    void testPurgeInvalid() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: this fails in Windows sometimes
        super.testPurgeInvalid();
    }

    @Test
    @Override
    void testPurgeWithIdentifier() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: this fails in Windows sometimes
        super.testPurgeWithIdentifier();
    }

}
