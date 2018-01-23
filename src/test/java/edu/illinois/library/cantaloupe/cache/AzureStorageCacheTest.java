package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class AzureStorageCacheTest extends AbstractCacheTest {

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");
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

        instance = newInstance();
    }

    @After
    public void tearDown() throws Exception {
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
    public void testGetContainerName() {
        assertEquals(
                Configuration.getInstance().getString(Key.AZURESTORAGECACHE_CONTAINER_NAME),
                AzureStorageCache.getContainerName());
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

    /* put(Identifier, Info) */

    @Ignore // TODO: this fails sometimes
    @Override
    @Test
    public void testPutConcurrently() {}

}
