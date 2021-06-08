package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AzureStorageCacheTest extends AbstractCacheTest {

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
    private AzureStorageCache instance;
    private OperationList opList = new OperationList();

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

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.purge();
    }

    @Override
    AzureStorageCache newInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "test/");
        config.setProperty(Key.AZURESTORAGECACHE_ACCOUNT_NAME, getAccountName());
        config.setProperty(Key.AZURESTORAGECACHE_ACCOUNT_KEY, getAccountKey());
        config.setProperty(Key.AZURESTORAGECACHE_CONTAINER_NAME, getContainer());

        return new AzureStorageCache();
    }

    /* getContainerName() */

    @Test
    void testGetContainerName() {
        assertEquals(
                Configuration.getInstance().getString(Key.AZURESTORAGECACHE_CONTAINER_NAME),
                AzureStorageCache.getContainerName());
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

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "/");
        assertEquals("", instance.getObjectKeyPrefix());

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "cats");
        assertEquals("cats/", instance.getObjectKeyPrefix());

        config.setProperty(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX, "cats/");
        assertEquals("cats/", instance.getObjectKeyPrefix());
    }

}
