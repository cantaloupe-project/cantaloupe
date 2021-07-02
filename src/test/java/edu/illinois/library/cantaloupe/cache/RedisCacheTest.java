package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

public class RedisCacheTest extends AbstractCacheTest {

    @Override
    RedisCache newInstance() {
        Configuration config = Configuration.getInstance();
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        config.setProperty(Key.REDISCACHE_HOST,
                testConfig.getString(ConfigurationConstants.REDIS_HOST.getKey()));
        config.setProperty(Key.REDISCACHE_PORT,
                testConfig.getProperty(ConfigurationConstants.REDIS_PORT.getKey()));
        config.setProperty(Key.REDISCACHE_SSL,
                testConfig.getProperty(ConfigurationConstants.REDIS_SSL.getKey()));
        config.setProperty(Key.REDISCACHE_PASSWORD,
                testConfig.getString(ConfigurationConstants.REDIS_PASSWORD.getKey()));
        config.setProperty(Key.REDISCACHE_DATABASE,
                testConfig.getProperty(ConfigurationConstants.REDIS_DATABASE.getKey()));

        RedisCache instance = new RedisCache();
        instance.purge();
        return instance;
    }


    /* getInfo(Identifier) */

    /**
     * Override that does nothing, as validity is determined by Redis.
     */
    @Override
    @Test
    void testGetInfoWithExistingInvalidImage() {}

    /* newDerivativeImageInputStream(OperationList) */

    /**
     * Override that does nothing, as validity is determined by Redis.
     */
    @Override
    @Test
    void testNewDerivativeImageInputStreamWithNonzeroTTL() {}

    /* purgeInvalid() */

    /**
     * Override that does nothing, as validity is determined by Redis.
     */
    @Test
    @Override
    void testPurgeInvalid() {}

}
