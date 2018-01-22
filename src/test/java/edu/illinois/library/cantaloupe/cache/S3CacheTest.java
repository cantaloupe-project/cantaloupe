package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.services.s3.iterable.S3Objects;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class S3CacheTest extends BaseTest {

    /**
     * Time to wait for asynchronous uploads.
     */
    private final int UPLOAD_WAIT = 3000;

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private Info imageInfo = new Info(64, 56, Format.JPG);
    private S3Cache instance;
    private OperationList opList = new OperationList(identifier, Format.JPG);

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

    private static String getEndpoint() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ENDPOINT.getKey());
    }

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_TTL, 2);
        config.setProperty(Key.S3CACHE_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.S3CACHE_BUCKET_NAME, getBucket());
        config.setProperty(Key.S3CACHE_ENDPOINT, getEndpoint());
        config.setProperty(Key.S3CACHE_OBJECT_KEY_PREFIX, "test/");
        config.setProperty(Key.S3CACHE_SECRET_KEY, getSecretKey());

        instance = new S3Cache();
        instance.initialize();
    }

    @After
    public void tearDown() throws Exception {
        instance.purge();
        instance.shutdown();
    }

    private void assertObjectCount(int count) {
        S3Objects objects = S3Objects.inBucket(
                S3Cache.getClientInstance(),
                instance.getBucketName());
        final AtomicInteger atint = new AtomicInteger(0);
        objects.iterator().forEachRemaining(t -> atint.incrementAndGet());
        assertEquals(count, atint.get());
    }

    /* getBucketName() */

    @Test
    public void testGetBucketName() {
        assertEquals(
                Configuration.getInstance().getString(Key.S3CACHE_BUCKET_NAME),
                instance.getBucketName());
    }

    /* getImageInfo() */

    @Test
    public void testGetImageInfo() throws Exception {
        instance.put(identifier, imageInfo);

        Info actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

    @Test
    public void testGetImageInfoWithNonexistentInfo() throws Exception {
        assertNull(instance.getImageInfo(identifier));
    }

    @Test
    public void testGetImageInfoWithInvalidInfo() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_TTL, 1);

        instance.put(identifier, imageInfo);

        Thread.sleep(3100);

        assertNull(instance.getImageInfo(identifier));
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStream() throws Exception {
        Path fixture = TestUtil.getImage(identifier.toString());

        // add an image
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, os);
        }

        // wait for it to upload
        Thread.sleep(UPLOAD_WAIT);

        // download the image
        try (InputStream s3is = instance.newDerivativeImageInputStream(opList)) {
            ByteArrayOutputStream s3ByteStream = new ByteArrayOutputStream();
            IOUtils.copy(s3is, s3ByteStream);
            s3ByteStream.close();

            // assert that the downloaded byte array is the same length as the fixture
            assertEquals(Files.size(fixture), s3ByteStream.toByteArray().length);
        }
    }

    @Test
    public void testNewDerivativeImageInputStreamWithNonexistentImage()
            throws Exception {
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    @Test
    public void testNewDerivativeImageInputStreamWithInvalidImage()
            throws Exception {
        Path fixture = TestUtil.getImage(identifier.toString());

        // add an image
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
        }

        // wait for it to upload
        Thread.sleep(UPLOAD_WAIT);

        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        }

        // wait for it to invalidate
        Thread.sleep(2100);

        // assert that it has been purged
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStream() throws Exception {
        assertObjectCount(0);

        // add an image
        Path imageFile = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile, outputStream);
        }

        Thread.sleep(UPLOAD_WAIT);

        assertObjectCount(1);
    }

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

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // add an image
        Path imageFile = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile, outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        Thread.sleep(UPLOAD_WAIT);

        assertObjectCount(2);

        // purge it
        instance.purge();

        assertObjectCount(0);
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        // add an image
        Path fixture1 = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture1, outputStream);
        }

        // add another image
        Path fixture2 = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture2.getFileName().toString()), Format.GIF);

        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(otherOpList)) {
            Files.copy(fixture2, outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        Thread.sleep(UPLOAD_WAIT);

        assertObjectCount(3);

        // purge an image
        instance.purge(opList);

        assertObjectCount(2);
    }

    /* purgeInvalid() */

    @Test
    public void testPurgeInvalid() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 4);

        // add an image
        Path fixture1 = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture1, outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        Thread.sleep(2000);

        // add another image
        Path fixture2 = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture2.getFileName().toString()), Format.GIF);

        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(otherOpList)) {
            Files.copy(fixture2, outputStream);
        }

        // add another Info
        Identifier otherId = new Identifier("cats");
        Info otherInfo = new Info(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        Thread.sleep(UPLOAD_WAIT);

        assertObjectCount(4);

        // purge it
        instance.purgeInvalid();

        assertObjectCount(2);
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        // add an image
        Path imageFile = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile, outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        // add another Info
        Identifier otherId = new Identifier("cats");
        Info otherInfo = new Info(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        Thread.sleep(UPLOAD_WAIT);

        assertObjectCount(3);

        // purge
        instance.purge(identifier);

        assertObjectCount(1);
    }

    /* put(Info) */

    @Test
    public void testPut() throws Exception {
        instance.put(identifier, imageInfo);

        Info actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

}
