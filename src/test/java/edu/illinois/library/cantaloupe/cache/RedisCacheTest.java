package edu.illinois.library.cantaloupe.cache;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class RedisCacheTest extends BaseTest {

    /**
     * Thread-safely initializes a shared connection.
     */
    private static class LazyConnectionHolder {
        static StatefulRedisConnection<String, byte[]> connection;
        static {
            org.apache.commons.configuration.Configuration testConfig =
                    TestUtil.getTestConfig();
            RedisURI redisUri = RedisURI.Builder.
                    redis(testConfig.getString(ConfigurationConstants.REDIS_HOST.getKey(), "localhost")).
                    withPort(testConfig.getInt(ConfigurationConstants.REDIS_PORT.getKey(), 6379)).
                    withSsl(testConfig.getBoolean(ConfigurationConstants.REDIS_SSL.getKey(), false)).
                    withPassword(testConfig.getString(ConfigurationConstants.REDIS_PASSWORD.getKey())).
                    withDatabase(testConfig.getInt(ConfigurationConstants.REDIS_DATABASE.getKey(), 0)).
                    build();
            RedisClient client = RedisClient.create(redisUri);
            connection = client.connect(new RedisCache.CustomRedisCodec());
        }
    }

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private RedisCache instance;

    private static StatefulRedisConnection<String, byte[]> getConnection() {
        return RedisCacheTest.LazyConnectionHolder.connection;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

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

        instance = new RedisCache();
    }

    @After
    public void tearDown() throws Exception {
        instance.purge();
    }

    /* getImageInfo(Identifier) */

    @Test
    public void testGetImageInfo() throws Exception {
        Identifier identifier = new Identifier("birds");

        assertNull(instance.getImageInfo(identifier));

        Info info = new Info(52, 52);
        instance.put(identifier, info);

        assertEquals(info, instance.getImageInfo(identifier));
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStream() throws Exception {
        OperationList opList = new OperationList(new Identifier("cats"), Format.JPG);
        Path imageFile = TestUtil.getImage(IMAGE);

        // Write an image to the cache
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile, outputStream);
        }

        // Read it back in
        InputStream inputStream = instance.newDerivativeImageInputStream(opList);
        byte[] imageBytes = new byte[(int) Files.size(imageFile)];
        IOUtils.readFully(inputStream, imageBytes);
        inputStream.close();

        assertEquals(Files.size(imageFile), imageBytes.length);
    }

    @Test
    public void testnewDerivativeImageInputStreamWithNonexistentImage()
            throws Exception {
        OperationList opList = new OperationList(new Identifier("bogus"), Format.TIF);
        assertNull(instance.newDerivativeImageInputStream(opList));
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStream() {
        // tested in testNewDerivativeImageInputStream()
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // Create some fixture data
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("birds");

        // Create an info for each identifier
        instance.put(id1, new Info(52, 52));
        instance.put(id2, new Info(52, 52));
        instance.put(id3, new Info(52, 52));

        // Create an image for each identifier...
        byte[] imageBytes = Files.readAllBytes(TestUtil.getImage(IMAGE));

        // ...image 1
        OperationList opList1 = new OperationList(id1, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList1.toString(), imageBytes);
        // ...image 2
        OperationList opList2 = new OperationList(id2, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList2.toString(), imageBytes);
        // ...image 3
        OperationList opList3 = new OperationList(id3, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList3.toString(), imageBytes);

        instance.purge();

        final String[] keys = new String[] { RedisCache.IMAGE_HASH_KEY,
                RedisCache.INFO_HASH_KEY };
        assertEquals(0, (long) getConnection().sync().exists(keys));
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        // Create some fixture data
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("birds");

        // Create an info for each identifier
        instance.put(id1, new Info(52, 52));
        instance.put(id2, new Info(52, 52));
        instance.put(id3, new Info(52, 52));

        // Create an image for each identifier...
        byte[] imageBytes = Files.readAllBytes(TestUtil.getImage(IMAGE));

        // ...image 1
        OperationList opList1 = new OperationList(id1, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList1.toString(), imageBytes);
        // ...image 2
        OperationList opList2 = new OperationList(id2, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList2.toString(), imageBytes);
        // ...image 3
        OperationList opList3 = new OperationList(id3, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList3.toString(), imageBytes);

        instance.purge(opList2);

        assertNotNull(instance.getImageInfo(id1));
        assertNotNull(instance.getImageInfo(id2));
        assertNotNull(instance.getImageInfo(id3));

        assertNotNull(instance.newDerivativeImageInputStream(opList1));
        assertNull(instance.newDerivativeImageInputStream(opList2));
        assertNotNull(instance.newDerivativeImageInputStream(opList3));
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() {
        // Nothing to test
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        // Create some fixture data
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("birds");

        // Create an info for each identifier
        instance.put(id1, new Info(52, 52));
        instance.put(id2, new Info(52, 52));
        instance.put(id3, new Info(52, 52));

        // Create an image for each identifier...
        byte[] imageBytes = Files.readAllBytes(TestUtil.getImage(IMAGE));

        // ...image 1
        OperationList opList1 = new OperationList(id1, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList1.toString(), imageBytes);
        // ...image 2
        OperationList opList2 = new OperationList(id2, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList2.toString(), imageBytes);
        // ...image 3
        OperationList opList3 = new OperationList(id3, Format.JPG);
        getConnection().sync().hset(RedisCache.IMAGE_HASH_KEY,
                opList3.toString(), imageBytes);

        // Purge one of the identifiers
        instance.purge(id2);

        assertNotNull(instance.getImageInfo(id1));
        assertNull(instance.getImageInfo(id2));
        assertNotNull(instance.getImageInfo(id3));

        assertNotNull(instance.newDerivativeImageInputStream(opList1));
        assertNull(instance.newDerivativeImageInputStream(opList2));
        assertNotNull(instance.newDerivativeImageInputStream(opList3));
    }

    /* put(Identifier, Info) */

    @Test
    public void testPut() throws Exception {
        Identifier identifier = new Identifier("birds");

        assertNull(instance.getImageInfo(identifier));

        Info info = new Info(52, 52);
        instance.put(identifier, info);

        assertEquals(info, instance.getImageInfo(identifier));
    }

}
