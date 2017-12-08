package edu.illinois.library.cantaloupe.cache;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AzureStorageCacheTest extends BaseTest {

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private Info imageInfo = new Info(64, 56, Format.JPG);
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

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_TTL, 1);
        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "test/");
        config.setProperty(Key.AZURESTORAGECACHE_ACCOUNT_NAME, getAccountName());
        config.setProperty(Key.AZURESTORAGECACHE_ACCOUNT_KEY, getAccountKey());
        config.setProperty(Key.AZURESTORAGECACHE_CONTAINER_NAME, getContainer());

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
        final AtomicInteger atint = new AtomicInteger(0);
        container.listBlobs(instance.getObjectKeyPrefix(), true).
                forEach(t -> atint.incrementAndGet());
        assertEquals(count, atint.get());
    }

    /* getContainerName() */

    @Test
    public void testGetContainerName() {
        assertEquals(
                Configuration.getInstance().getString(Key.AZURESTORAGECACHE_CONTAINER_NAME),
                AzureStorageCache.getContainerName());
    }

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
        Path fixture = TestUtil.getImage(identifier.toString());

        // add an image
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
        }

        // download the image
        InputStream s3InputStream = instance.newDerivativeImageInputStream(opList);
        ByteArrayOutputStream s3ByteStream = new ByteArrayOutputStream();
        IOUtils.copy(s3InputStream, s3ByteStream);
        s3InputStream.close();
        s3ByteStream.close();

        // assert that the downloaded byte array is the same size as the fixture
        assertEquals(Files.size(fixture), s3ByteStream.toByteArray().length);
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
        Path fixture = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
        }

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

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "/");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "cats");
        assertEquals("cats/", instance.getObjectKeyPrefix());

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "cats/");
        assertEquals("cats/", instance.getObjectKeyPrefix());
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // add an image
        Path fixture = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
        }

        // add an Info
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
        Path fixture = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
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

        assertObjectCount(3);

        // purge an image
        instance.purge(opList);

        assertObjectCount(2);
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 2);

        // add an image
        Path fixture = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
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

        assertObjectCount(4);

        instance.purgeExpired();

        assertObjectCount(2);
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        // add an image
        Path fixture = TestUtil.getImage(identifier.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, outputStream);
        }

        // add an Info
        instance.put(identifier, imageInfo);

        // add another Info
        Identifier otherId = new Identifier("cats");
        Info otherInfo = new Info(64, 56, Format.GIF);
        instance.put(otherId, otherInfo);

        assertObjectCount(3);

        // purge an info
        instance.purge(identifier);

        assertObjectCount(2);
    }

    /* put(Info) */

    @Test
    public void testPutWithImageInfo() throws Exception {
        instance.put(identifier, imageInfo);
        Info actualInfo = instance.getImageInfo(identifier);
        assertEquals(imageInfo.toString(), actualInfo.toString());
    }

}
