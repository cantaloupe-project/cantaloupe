package edu.illinois.library.cantaloupe.resolver;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Tests AzureStorageResolver against Azure Storage. (Requires an Azure Storage
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

        final File fixture = TestUtil.getImage("jpg-rgb-64x56x8-line.jpg");
        try (OutputStream os = blob.openOutputStream()) {
            Files.copy(fixture.toPath(), os);
        }
    }

    @AfterClass
    public static void removeFixtures() throws Exception {
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

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.AZURESTORAGERESOLVER_CONTAINER_NAME,
                getContainer());
        config.setProperty(Key.AZURESTORAGERESOLVER_ACCOUNT_NAME,
                getAccountName());
        config.setProperty(Key.AZURESTORAGERESOLVER_ACCOUNT_KEY,
                getAccountKey());
        config.setProperty(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        instance = new AzureStorageResolver();
        instance.setIdentifier(new Identifier(OBJECT_KEY));
        instance.setContext(new RequestContext());
    }

    @Test
    public void testNewStreamSourceWithBasicLookupStrategy() {
        // present, readable image
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testNewStreamSourceWithScriptLookupStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        // present image
        try {
            StreamSource source = instance.newStreamSource();
            assertNotNull(source.newInputStream());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetSourceFormatWithBasicLookupStrategy() throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        try {
            instance.setIdentifier(new Identifier("image"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testGetSourceFormatWithScriptLookupStrategy() throws IOException {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        // present image
        assertEquals(Format.JPG, instance.getSourceFormat());
        // present image without extension TODO: write this

        // missing image with extension
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        // missing image without extension
        try {
            instance.setIdentifier(new Identifier("image"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

}
