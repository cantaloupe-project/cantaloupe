package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.request.Identifier;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;

public class JdbcCacheTest extends CantaloupeTestCase {

    JdbcCache instance;

    public void setUp() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        // use an in-memory H2 database
        config.setProperty(JdbcCache.CONNECTION_STRING_CONFIG_KEY,
                "jdbc:h2:mem:test");
        config.setProperty(JdbcCache.USER_CONFIG_KEY, "sa");
        config.setProperty(JdbcCache.PASSWORD_CONFIG_KEY, "");
        config.setProperty(JdbcCache.IMAGE_TABLE_CONFIG_KEY, "image_cache");
        config.setProperty(JdbcCache.INFO_TABLE_CONFIG_KEY, "info_cache");
        config.setProperty(JdbcCache.TTL_CONFIG_KEY, 0);
        Application.setConfiguration(config);

        instance = new JdbcCache();

        // persist some images
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        OutputStream os = instance.getImageOutputStream(params);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();

        params = new Parameters("dogs", "50,50,50,50", "pct:90",
                "0", "default", "jpg");
        os = instance.getImageOutputStream(params);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();

        params = new Parameters("bunnies", "10,20,50,90", "40,",
                "15", "color", "png");
        os = instance.getImageOutputStream(params);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();

        // persist some corresponding dimensions
        instance.putDimension(new Identifier("cats"), new Dimension(50, 40));
        instance.putDimension(new Identifier("dogs"), new Dimension(500, 300));
        instance.putDimension(new Identifier("bunnies"), new Dimension(350, 240));

        // assert that the data has been seeded
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                JdbcCache.IMAGE_TABLE_PARAMS_COLUMN,
                config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
        PreparedStatement statement = JdbcCache.getConnection().prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            assertEquals(3, resultSet.getInt("count"));
        } else {
            fail();
        }

        sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
        statement = JdbcCache.getConnection().prepareStatement(sql);
        resultSet = statement.executeQuery();
        if (resultSet.next()) {
            assertEquals(3, resultSet.getInt("count"));
        } else {
            fail();
        }
    }

    /**
     * Clears the persistent store.
     */
    public void tearDown() throws IOException {
        instance.flush();
    }

    public void testFlush() throws Exception {
        Configuration config = Application.getConfiguration();

        instance.flush();

        // assert that the images and infos were flushed
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_PARAMS_COLUMN,
                config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
        PreparedStatement statement = JdbcCache.getConnection().prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(0, resultSet.getInt("count"));

        sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
        statement = JdbcCache.getConnection().prepareStatement(sql);
        resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(0, resultSet.getInt("count"));
    }

    public void testFlushWithParameters() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        instance.flush(params);

        Configuration config = Application.getConfiguration();

        // assert that the image and info were flushed
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_PARAMS_COLUMN,
                config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
        PreparedStatement statement = JdbcCache.getConnection().prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(2, resultSet.getInt("count"));

        sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
        statement = JdbcCache.getConnection().prepareStatement(sql);
        resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(2, resultSet.getInt("count"));
    }

    public void testFlushExpired() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        Parameters params = new Parameters("bees", "full", "full", "0",
                "default", "jpg");
        OutputStream os = instance.getImageOutputStream(params);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        instance.flushExpired();

        // assert that only the expired images and infos were flushed
        Configuration config = Application.getConfiguration();
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_PARAMS_COLUMN,
                config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
        PreparedStatement statement = JdbcCache.getConnection().prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(1, resultSet.getInt("count"));

        sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_PARAMS_COLUMN,
                config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
        statement = JdbcCache.getConnection().prepareStatement(sql);
        resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(1, resultSet.getInt("count"));
    }

    public void testGetDimensionWithZeroTtl() throws IOException {
        // existing image
        try {
            Dimension actual = instance.getDimension(new Identifier("cats"));
            Dimension expected = new Dimension(50, 40);
            assertEquals(actual, expected);
        } catch (IOException e) {
            fail();
        }
        // nonexistent image
        assertNull(instance.getDimension(new Identifier("bogus")));
    }

    public void testGetDimensionWithNonZeroTtl() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        Parameters params = new Parameters("bees", "full", "full", "0",
                "default", "jpg");
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")),
                instance.getImageOutputStream(params));
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        // existing, non-expired image
        try {
            Dimension actual = instance.getDimension(new Identifier("bees"));
            Dimension expected = new Dimension(50, 40);
            assertEquals(actual, expected);
        } catch (IOException e) {
            fail();
        }
        // existing, expired image
        assertNull(instance.getDimension(new Identifier("cats")));
        // nonexistent image
        assertNull(instance.getDimension(new Identifier("bogus")));
    }

    public void testGetImageInputStreamWithZeroTtl() {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        assertNotNull(instance.getImageInputStream(params));
    }

    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        Parameters params = new Parameters("bees", "full", "full", "0",
                "default", "jpg");
        OutputStream os = instance.getImageOutputStream(params);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        // existing, non-expired image
        assertNotNull(instance.getImageInputStream(params));
        // existing, expired image
        assertNull(instance.getImageInputStream(
                Parameters.fromUri("cats/full/full/0/default.jpg")));
        // nonexistent image
        assertNull(instance.getImageInputStream(
                Parameters.fromUri("bogus/full/full/0/default.jpg")));
    }

    public void testGetImageOutputStream() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        assertNotNull(instance.getImageOutputStream(params));
    }

    public void testOldestValidDate() {
        // ttl = 0
        assertEquals(new Date(Long.MIN_VALUE), instance.oldestValidDate());
        // ttl = 50
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 50);
        long expectedTime = Date.from(Instant.now().minus(Duration.ofSeconds(50))).getTime();
        long actualTime = instance.oldestValidDate().getTime();
        assertTrue(Math.abs(actualTime - expectedTime) < 100);
    }

    public void testPutDimension() throws IOException {
        Identifier identifier = new Identifier("birds");
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

}
