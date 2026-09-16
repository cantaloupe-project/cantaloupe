package edu.illinois.library.cantaloupe.source;

import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.AzureStorageTestUtil;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.NoSuchFileException;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests AzureStorageSource against Azure Storage. (Requires an Azure
 * account.)
 */
public class AzureStorageSourceTest extends AbstractSourceTest {

    private AzureStorageSource instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        AzureStorageTestUtil.uploadFixtures();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        AzureStorageTestUtil.deleteFixtures();
    }

    private static void clearConfig() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CONTAINER_NAME, "");
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_NAME, "");
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_KEY, "");
    }

    private static String generateSAS() {
        AccountSasPermission permissions = new AccountSasPermission().
                setReadPermission(true).
                setWritePermission(true).
                setListPermission(true);
        AccountSasService services = new AccountSasService().
                setBlobAccess(true).
                setFileAccess(true);
        AccountSasResourceType resourceTypes = new AccountSasResourceType().
                setObject(true);
        AccountSasSignatureValues values = new AccountSasSignatureValues(
                OffsetDateTime.now().plusYears(100), permissions, services,
                resourceTypes).setProtocol(SasProtocol.HTTPS_ONLY);

        return AzureStorageSource.getAccount().generateAccountSas(values);
    }

    private static String getAccountName() {
        org.apache.commons.configuration2.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_NAME.getKey());
    }

    private static String getAccountKey() {
        org.apache.commons.configuration2.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_KEY.getKey());
    }

    private static String getContainer() {
        org.apache.commons.configuration2.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_CONTAINER.getKey());
    }

    private static String getSASURI() {
        return String.format("https://%s.blob.core.windows.net/%s/%s?%s",
                AzureStorageTestUtil.getAccountName(),
                AzureStorageTestUtil.getContainer(),
                AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                generateSAS());
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    void destroyEndpoint() {
        // will be done in @AfterAll
    }

    @Override
    void initializeEndpoint() {
        // will be done in @BeforeAll
    }

    @Override
    AzureStorageSource newInstance() {
        AzureStorageSource instance = new AzureStorageSource();
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CONTAINER_NAME,
                getContainer());
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_NAME,
                getAccountName());
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_KEY,
                getAccountKey());
        config.setProperty(Key.AZURESTORAGESOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.AZURESTORAGESOURCE_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");

            Identifier identifier = new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
            DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
            delegateProxy.getRequestContext().setIdentifier(identifier);
            instance.setDelegateProxy(delegateProxy);
        } catch (Exception e) {
            fail();
        }
    }

    /* checkAccess() */

    @Test
    void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.stat();
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage() {
        useScriptLookupStrategy();
        // TODO: write this
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingImage() {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
        delegateProxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(delegateProxy);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, instance::stat);
    }

    @Test
    void testCheckAccessWithSAS() throws Exception {
        instance.setIdentifier(new Identifier(getSASURI()));
        clearConfig();
        instance.stat();
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorHasNext() {
        AzureStorageSource source = newInstance();
        source.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));

        AzureStorageSource.FormatIterator<Format> it =
                source.getFormatIterator();
        assertTrue(it.hasNext());
        it.next(); // object key
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // Content-Type is null
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void testGetFormatIteratorNext() {
        AzureStorageSource source = newInstance();
        source.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION));

        AzureStorageSource.FormatIterator<Format> it = source.getFormatIterator();
        assertEquals(Format.get("png"), it.next());     // object key
        assertEquals(Format.get("png"), it.next());     // identifier extension
        assertEquals(Format.UNKNOWN, it.next()); // Content-Type is null
        assertEquals(Format.get("jpg"), it.next());     // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryUsingBasicLookupStrategy() throws Exception {
        instance.newStreamFactory();
    }

    @Test
    void testNewStreamFactoryUsingScriptLookupStrategy() throws Exception {
        useScriptLookupStrategy();
        assertNotNull(instance.newStreamFactory());
    }

    @Test
    void testNewStreamFactoryWithSAS() throws Exception {
        instance.setIdentifier(new Identifier(getSASURI()));
        clearConfig();
        instance.newStreamFactory();
    }

    /* getBlobKey() */

    @Test
    void testGetBlobKey() throws Exception {
        assertNotNull(instance.getBlobKey());
    }
    @Test
    void testGetBlobKeyBasicLookupStrategy() throws Exception {
        String result = instance.getBlobKey();
        assertEquals(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION, result);
    }
    @Test
    void testGetObjectKeyDelegateLookupStrategy() throws Exception {
        useScriptLookupStrategy();
        String result = instance.getBlobKey();
        assertEquals(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION, result);
    }
}
