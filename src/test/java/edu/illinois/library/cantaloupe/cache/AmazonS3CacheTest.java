package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class AmazonS3CacheTest extends BaseTest {

    private final int S3_UPLOAD_WAIT = 3000;

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private Info imageInfo = new Info(64, 56, Format.JPG);
    private AmazonS3Cache instance;
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

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.CACHE_SERVER_TTL, 2);
        config.setProperty(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX, "test/");
        config.setProperty(Key.AMAZONS3CACHE_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.AMAZONS3CACHE_BUCKET_NAME, getBucket());
        config.setProperty(Key.AMAZONS3CACHE_SECRET_KEY, getSecretKey());
        config.setProperty(Key.AMAZONS3CACHE_BUCKET_REGION, getRegion());

        instance = new AmazonS3Cache();
    }

    @After
    public void tearDown() throws Exception {
        instance.purge();
    }

    private void assertObjectCount(int count) {
        S3Objects objects = S3Objects.inBucket(
                AmazonS3Cache.getClientInstance(),
                instance.getBucketName());
        int i = 0;
        for (S3ObjectSummary summary : objects) {
            i++;
        }
        assertEquals(count, i);
    }

    /* getBucketName() */

    @Test
    public void testGetBucketName() {
        assertEquals(
                ConfigurationFactory.getInstance().getString(Key.AMAZONS3CACHE_BUCKET_NAME),
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

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStream() throws Exception {
        File fixture = TestUtil.getImage(identifier.toString());

        // add an image
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture.toPath(), outputStream);
        }

        // wait for it to upload
        Thread.sleep(S3_UPLOAD_WAIT);

        // download the image
        InputStream s3InputStream = instance.newDerivativeImageInputStream(opList);
        ByteArrayOutputStream s3ByteStream = new ByteArrayOutputStream();
        IOUtils.copy(s3InputStream, s3ByteStream);
        s3InputStream.close();
        s3ByteStream.close();

        // assert that the downloaded byte array is the same size as the fixture
        assertEquals(fixture.length(), s3ByteStream.toByteArray().length);
    }

    @Test
    public void testnewDerivativeImageInputStreamWithNonexistentImage()
            throws Exception {
        assertNull(instance.newDerivativeImageInputStream(opList));
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStream() throws Exception {
        assertObjectCount(0);

        // add an image
        File imageFile = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile.toPath(), outputStream);
        }

        Thread.sleep(S3_UPLOAD_WAIT);

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
        Configuration config = ConfigurationFactory.getInstance();

        config.setProperty(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX, "");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX, "/");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX, "cats");
        assertEquals("cats/", instance.getObjectKeyPrefix());

        config.setProperty(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX, "cats/");
        assertEquals("cats/", instance.getObjectKeyPrefix());
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // add an image
        File imageFile = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile.toPath(), outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        assertObjectCount(2);

        // purge it
        instance.purge();

        assertObjectCount(0);
    }

    /* purge(OperationList */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        // add an image
        File fixture1 = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture1.toPath(), outputStream);
        }

        // add another image
        File fixture2 = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture2.getName()), Format.GIF);

        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(otherOpList)) {
            Files.copy(fixture2.toPath(), outputStream);
        }

        Thread.sleep(S3_UPLOAD_WAIT);

        // add an Info
        instance.put(identifier, imageInfo);

        assertObjectCount(3);

        // purge an image
        instance.purge(opList);

        assertObjectCount(2);
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        ConfigurationFactory.getInstance().setProperty(Key.CACHE_SERVER_TTL, 4);

        // add an image
        File fixture1 = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture1.toPath(), outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        Thread.sleep(2000);

        // add another image
        File fixture2 = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture2.getName()), Format.GIF);

        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(otherOpList)) {
            Files.copy(fixture2.toPath(), outputStream);
        }

        // add another Info
        Identifier otherId = new Identifier("cats");
        Info otherInfo = new Info(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        Thread.sleep(S3_UPLOAD_WAIT);

        assertObjectCount(4);

        // purge it
        instance.purgeExpired();

        assertObjectCount(2);
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        // add an image
        File imageFile = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile.toPath(), outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        // add another Info
        Identifier otherId = new Identifier("cats");
        Info otherInfo = new Info(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        assertObjectCount(3);

        // purge
        instance.purge(identifier);

        assertObjectCount(1);
    }

    /* put(Info) */

    @Test
    public void testPutWithImageInfo() throws Exception {
        instance.put(identifier, imageInfo);
        Info actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

}
