package edu.illinois.library.cantaloupe.resolver;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Tests AzureStorageResolver against Azure Storage. (Requires an Azure
 * account.)
 */
public class AzureStorageResolverTest extends BaseTest {

    private static final String OBJECT_KEY = "jpeg.jpg";

    private AzureStorageResolver instance;

    @BeforeClass
    public static void uploadFixtures() throws Exception {
        final CloudBlobClient client = client();
        final CloudBlobContainer container =
                client.getContainerReference(getContainer());
        final CloudBlockBlob blob = container.getBlockBlobReference(OBJECT_KEY);
        blob.getProperties().setContentType("image/jpeg");

        final Path fixture = TestUtil.getImage("jpg-rgb-64x56x8-line.jpg");
        try (OutputStream os = blob.openOutputStream()) {
            Files.copy(fixture, os);
        }
    }

    @AfterClass
    public static void deleteFixtures() throws Exception {
        final CloudBlobClient client = client();
        final CloudBlobContainer container =
                client.getContainerReference(getContainer());
        final CloudBlockBlob blob = container.getBlockBlobReference(OBJECT_KEY);
        blob.deleteIfExists();
    }

    private static CloudBlobClient client() throws Exception {
        final String accountName = getAccountName();
        final String accountKey = getAccountKey();

        final String connectionString = String.format(
                "DefaultEndpointsProtocol=https;" +
                        "AccountName=%s;" +
                        "AccountKey=%s", accountName, accountKey);
        final CloudStorageAccount account =
                CloudStorageAccount.parse(connectionString);
        CloudBlobClient client = account.createCloudBlobClient();
        client.getContainerReference(getContainer()).createIfNotExists();
        return client;
    }

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

        useBasicLookupStrategy();

        instance = new AzureStorageResolver();
        instance.setIdentifier(new Identifier(OBJECT_KEY));
        instance.setContext(new RequestContext());
    }

    private void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGERESOLVER_CONTAINER_NAME,
                getContainer());
        config.setProperty(Key.AZURESTORAGERESOLVER_ACCOUNT_NAME,
                getAccountName());
        config.setProperty(Key.AZURESTORAGERESOLVER_ACCOUNT_KEY,
                getAccountKey());
        config.setProperty(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    private void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        instance.setIdentifier(new Identifier("bogus"));
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage() {
        useScriptLookupStrategy();
        // TODO: write this
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.setIdentifier(new Identifier("bogus"));
        instance.checkAccess();
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategy()
            throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategy()
            throws IOException {
        useScriptLookupStrategy();
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithImageWithRecognizedExtension()
            throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithImageWithUnrecognizedExtension() {
        // TODO: write this
    }

    @Test
    public void testGetSourceFormatWithImageWithNoExtension() {
        // TODO: write this
    }

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategy() throws Exception {
        instance.newStreamSource();
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategy()
            throws Exception {
        useScriptLookupStrategy();
        assertNotNull(instance.newStreamSource());
    }

}
