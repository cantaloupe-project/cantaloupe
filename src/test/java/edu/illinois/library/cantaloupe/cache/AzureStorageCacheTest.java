package edu.illinois.library.cantaloupe.cache;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class AzureStorageCacheTest extends BaseTest {

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private ImageInfo imageInfo = new ImageInfo(64, 56, Format.JPG);
    private AzureStorageCache instance;
    private OperationList opList = new OperationList(identifier, Format.JPG);

    private static String getAccountName() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_NAME.getKey());
    }

    private static String getAccountKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_KEY.getKey());
    }

    private static String getContainer() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_CONTAINER.getKey());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Cache.TTL_CONFIG_KEY, 1);
        config.setProperty(AzureStorageCache.OBJECT_KEY_PREFIX_CONFIG_KEY, "test/");
        config.setProperty(AzureStorageCache.ACCOUNT_NAME_CONFIG_KEY, getAccountName());
        config.setProperty(AzureStorageCache.ACCOUNT_KEY_CONFIG_KEY, getAccountKey());
        config.setProperty(AzureStorageCache.CONTAINER_NAME_CONFIG_KEY, getContainer());

        instance = new AzureStorageCache();
    }

    @After
    public void tearDown() throws Exception {
        instance.purge();
    }

    private void assertObjectCount(int count)
            throws StorageException, URISyntaxException {
        CloudBlobClient client = AzureStorageCache.getClientInstance();
        final CloudBlobContainer container =
                client.getContainerReference(AzureStorageCache.getContainerName());
        int i = 0;
        for (ListBlobItem item : container.listBlobs(instance.getObjectKeyPrefix(), true)) {
            i++;
        }
        assertEquals(count, i);
    }

    /* getContainerName() */

    @Test
    public void testGetContainerName() {
        assertEquals(
                ConfigurationFactory.getInstance().
                        getString(AzureStorageCache.CONTAINER_NAME_CONFIG_KEY),
                AzureStorageCache.getContainerName());
    }

    @Test
    public void testGetImageInfo() throws Exception {
        instance.put(identifier, imageInfo);
        ImageInfo actualInfo = instance.getImageInfo(identifier);
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
        InputStream fileInputStream = new FileInputStream(fixture);
        OutputStream outputStream = instance.newDerivativeImageOutputStream(opList);
        IOUtils.copy(fileInputStream, outputStream);
        fileInputStream.close();
        outputStream.close();

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
    public void testNewDerivativeImageInputStreamWithNonexistentImage()
            throws Exception {
        assertNull(instance.newDerivativeImageInputStream(opList));
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStream() throws Exception {
        assertObjectCount(0);

        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.newDerivativeImageOutputStream(opList);
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
        Configuration config = ConfigurationFactory.getInstance();

        config.setProperty(AzureStorageCache.OBJECT_KEY_PREFIX_CONFIG_KEY, "");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(AzureStorageCache.OBJECT_KEY_PREFIX_CONFIG_KEY, "/");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(AzureStorageCache.OBJECT_KEY_PREFIX_CONFIG_KEY, "cats");
        assertEquals("cats/", instance.getObjectKeyPrefix());

        config.setProperty(AzureStorageCache.OBJECT_KEY_PREFIX_CONFIG_KEY, "cats/");
        assertEquals("cats/", instance.getObjectKeyPrefix());
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.newDerivativeImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.put(identifier, imageInfo);

        assertObjectCount(2);

        // purge it
        instance.purge();

        assertObjectCount(0);
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.newDerivativeImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add another image
        File fixture = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture.getName()), Format.GIF);
        inputStream = new FileInputStream(fixture);
        outputStream = instance.newDerivativeImageOutputStream(otherOpList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.put(identifier, imageInfo);

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
        OutputStream outputStream = instance.newDerivativeImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.put(identifier, imageInfo);

        Thread.sleep(2000);

        // add another image
        File fixture = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList otherOpList = new OperationList(
                new Identifier(fixture.getName()), Format.GIF);
        inputStream = new FileInputStream(fixture);
        outputStream = instance.newDerivativeImageOutputStream(otherOpList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add another ImageInfo
        Identifier otherId = new Identifier("cats");
        ImageInfo otherInfo = new ImageInfo(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        assertObjectCount(4);

        // purge it
        instance.purgeExpired();

        assertObjectCount(2);
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        // add an image
        InputStream inputStream = new FileInputStream(
                TestUtil.getImage(identifier.toString()));
        OutputStream outputStream = instance.newDerivativeImageOutputStream(opList);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // add an ImageInfo
        instance.put(identifier, imageInfo);

        // add another ImageInfo
        Identifier otherId = new Identifier("cats");
        ImageInfo otherInfo = new ImageInfo(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        assertObjectCount(3);

        // purge an info
        instance.purge(identifier);

        assertObjectCount(2);
    }

    /* put(ImageInfo) */

    @Test
    public void testPutWithImageInfo() throws Exception {
        instance.put(identifier, imageInfo);
        ImageInfo actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

}
