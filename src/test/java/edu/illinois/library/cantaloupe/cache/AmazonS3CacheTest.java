package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

public class AmazonS3CacheTest {

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private ImageInfo imageInfo = new ImageInfo(64, 56, Format.JPG);
    private AmazonS3Cache instance;
    private OperationList opList = new OperationList(identifier, Format.JPG);

    @Before
    public void setUp() throws Exception {
        FileInputStream fis = new FileInputStream(new File(
                System.getProperty("user.home") + "/.s3/cantaloupe"));
        String authInfo = IOUtils.toString(fis);
        String[] lines = StringUtils.split(authInfo, "\n");

        final String accessKeyId = lines[0].replace("AWSAccessKeyId=", "").trim();
        final String secretKey = lines[1].replace("AWSSecretKey=", "").trim();
        final String bucketName = lines[3].replace("TestBucket=", "").trim();

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(AmazonS3Cache.OBJECT_KEY_PREFIX_CONFIG_KEY, "test/");
        config.setProperty(AmazonS3Cache.TTL_SECONDS_CONFIG_KEY, 1);
        config.setProperty(AmazonS3Cache.ACCESS_KEY_ID_CONFIG_KEY, accessKeyId);
        config.setProperty(AmazonS3Cache.BUCKET_NAME_CONFIG_KEY, bucketName);
        //config.setProperty(AmazonS3Cache.BUCKET_REGION_CONFIG_KEY, "us-east-1");
        config.setProperty(AmazonS3Cache.SECRET_KEY_CONFIG_KEY, secretKey);
        Application.setConfiguration(config);

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
                Application.getConfiguration().getString(AmazonS3Cache.BUCKET_NAME_CONFIG_KEY),
                instance.getBucketName());
    }

    /* getImageInfo() */

    @Test
    public void testGetImageInfo() throws Exception {
        instance.putImageInfo(identifier, imageInfo);
        ImageInfo actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

    @Test
    public void testGetImageInfoWithNonexistentInfo() throws Exception {
        assertNull(instance.getImageInfo(identifier));
    }

    /* getImageInputStream(OperationList) */

    @Test
    public void testGetImageInputStream() throws Exception {
        File fixture = TestUtil.getImage(identifier.toString());

        // add an image
        InputStream fileInputStream = new FileInputStream(fixture);
        OutputStream outputStream = instance.getImageOutputStream(opList);
        IOUtils.copy(fileInputStream, outputStream);
        fileInputStream.close();
        outputStream.close();

        // download the image
        InputStream s3InputStream = instance.getImageInputStream(opList);
        ByteArrayOutputStream s3ByteStream = new ByteArrayOutputStream();
        IOUtils.copy(s3InputStream, s3ByteStream);
        s3InputStream.close();
        s3ByteStream.close();

        // assert that the downloaded byte array is the same size as the fixture
        assertEquals(fixture.length(), s3ByteStream.toByteArray().length);
    }

    @Test
    public void testGetImageInputStreamWithNonexistentImage() throws Exception {
        assertNull(instance.getImageInputStream(opList));
    }

    /* getImageOutputStream(OperationList) */

    @Test
    public void testGetImageOutputStream() throws Exception {
        assertObjectCount(0);

        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.getImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

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
        Configuration config = Application.getConfiguration();

        config.setProperty(AmazonS3Cache.OBJECT_KEY_PREFIX_CONFIG_KEY, "");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(AmazonS3Cache.OBJECT_KEY_PREFIX_CONFIG_KEY, "/");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(AmazonS3Cache.OBJECT_KEY_PREFIX_CONFIG_KEY, "cats");
        assertEquals("cats/", instance.getObjectKeyPrefix());

        config.setProperty(AmazonS3Cache.OBJECT_KEY_PREFIX_CONFIG_KEY, "cats/");
        assertEquals("cats/", instance.getObjectKeyPrefix());
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.getImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.putImageInfo(identifier, imageInfo);

        assertObjectCount(2);

        // purge it
        instance.purge();

        assertObjectCount(0);
    }

    /* purge(OperationList */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.getImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add another image
        File fixture = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture.getName()), Format.GIF);
        inputStream = new FileInputStream(fixture);
        outputStream = instance.getImageOutputStream(otherOpList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.putImageInfo(identifier, imageInfo);

        assertObjectCount(3);

        // purge an image
        instance.purge(opList);

        assertObjectCount(2);
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.getImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.putImageInfo(identifier, imageInfo);

        Thread.sleep(2000);

        // add another image
        File fixture = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture.getName()), Format.GIF);
        inputStream = new FileInputStream(fixture);
        outputStream = instance.getImageOutputStream(otherOpList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add another ImageInfo
        Identifier otherId = new Identifier("cats");
        ImageInfo otherInfo = new ImageInfo(64, 56, Format.GIF);
        instance.putImageInfo(otherId, otherInfo);

        assertObjectCount(4);

        // purge it
        instance.purgeExpired();

        assertObjectCount(2);
    }

    /* purgeImage(Identifier) */

    @Test
    public void testPurgeImage() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.getImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.putImageInfo(identifier, imageInfo);

        // add another ImageInfo
        Identifier otherId = new Identifier("cats");
        ImageInfo otherInfo = new ImageInfo(64, 56, Format.GIF);
        instance.putImageInfo(otherId, otherInfo);

        assertObjectCount(3);

        // purge
        instance.purgeImage(identifier);

        assertObjectCount(1);
    }

    /* putImageInfo(ImageInfo) */

    @Test
    public void testPutImageInfo() throws Exception {
        instance.putImageInfo(identifier, imageInfo);
        ImageInfo actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

}
