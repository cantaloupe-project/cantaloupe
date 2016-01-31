package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.*;

public class JdbcCacheTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    JdbcCache instance;

    @Before
    public void setUp() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        // use an in-memory H2 database
        config.setProperty(JdbcCache.JDBC_URL_CONFIG_KEY, "jdbc:h2:mem:test");
        config.setProperty(JdbcCache.USER_CONFIG_KEY, "sa");
        config.setProperty(JdbcCache.PASSWORD_CONFIG_KEY, "");
        config.setProperty(JdbcCache.IMAGE_TABLE_CONFIG_KEY, "image_cache");
        config.setProperty(JdbcCache.INFO_TABLE_CONFIG_KEY, "info_cache");
        config.setProperty(JdbcCache.TTL_CONFIG_KEY, 0);
        Application.setConfiguration(config);

        try (Connection connection = JdbcCache.getConnection()) {
            createTables(connection);
        }

        instance = new JdbcCache();

        // persist some images
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));

        OutputStream bc = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), bc);
        bc.close();

        Identifier identifier = new Identifier("dogs");
        Crop crop = new Crop();
        crop.setX(50f);
        crop.setY(50f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setPercent(0.9f);
        Rotate rotate = new Rotate();
        OutputFormat format = OutputFormat.JPG;
        ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        bc = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), bc);
        bc.close();

        identifier = new Identifier("bunnies");
        crop = new Crop();
        crop.setX(10f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(90f);
        scale = new Scale();
        scale.setWidth(40);
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        rotate = new Rotate(15);
        format = OutputFormat.PNG;
        ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        bc = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), bc);
        bc.close();

        // persist some corresponding dimensions
        instance.putDimension(new Identifier("cats"), new Dimension(50, 40));
        instance.putDimension(new Identifier("dogs"), new Dimension(500, 300));
        instance.putDimension(new Identifier("bunnies"), new Dimension(350, 240));

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the data has been seeded
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                    JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                assertEquals(3, resultSet.getInt("count"));
            } else {
                fail();
            }

            sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                assertEquals(3, resultSet.getInt("count"));
            } else {
                fail();
            }
        }
    }

    @After
    public void tearDown() throws CacheException {
        instance.purge();
    }

    private void createTables(Connection connection) throws Exception {
        // image table
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "%s VARCHAR(4096) NOT NULL, " +
                "%s BLOB, " +
                "%s DATETIME);",
                JdbcCache.getImageTableName(),
                JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                JdbcCache.IMAGE_TABLE_IMAGE_COLUMN,
                JdbcCache.IMAGE_TABLE_LAST_MODIFIED_COLUMN);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();

        // info table
        sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "%s VARCHAR(4096) NOT NULL, " +
                        "%s INTEGER, " +
                        "%s INTEGER, " +
                        "%s DATETIME);",
                JdbcCache.getInfoTableName(),
                JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                JdbcCache.INFO_TABLE_WIDTH_COLUMN,
                JdbcCache.INFO_TABLE_HEIGHT_COLUMN,
                JdbcCache.INFO_TABLE_LAST_MODIFIED_COLUMN);
        statement = connection.prepareStatement(sql);
        statement.execute();
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        Configuration config = Application.getConfiguration();

        instance.purge();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the images and infos were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(0, resultSet.getInt("count"));

            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(0, resultSet.getInt("count"));
        }
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        Configuration config = Application.getConfiguration();

        Identifier id1 = new Identifier("cats");
        instance.purge(id1);

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the images and infos were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));

            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));
        }
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperations() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        instance.purge(ops);

        Configuration config = Application.getConfiguration();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the image and info were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));

            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));
        }
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));

        OutputStream bc = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), bc);
        bc.close();
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        instance.purgeExpired();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that only the expired images and infos were purged
            Configuration config = Application.getConfiguration();
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(1, resultSet.getInt("count"));

            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(1, resultSet.getInt("count"));
        }
    }

    @Test
    public void testGetDimensionWithZeroTtl() throws CacheException {
        // existing image
        try {
            Dimension actual = instance.getDimension(new Identifier("cats"));
            Dimension expected = new Dimension(50, 40);
            assertEquals(actual, expected);
        } catch (CacheException e) {
            fail();
        }
        // nonexistent image
        assertNull(instance.getDimension(new Identifier("bogus")));
    }

    @Test
    public void testGetDimensionWithNonZeroTtl() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bees"));

        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)),
                instance.getImageOutputStream(ops));
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        // existing, non-expired image
        try {
            Dimension actual = instance.getDimension(new Identifier("bees"));
            Dimension expected = new Dimension(50, 40);
            assertEquals(actual, expected);
        } catch (CacheException e) {
            fail();
        }
        // existing, expired image
        assertNull(instance.getDimension(new Identifier("cats")));
        // nonexistent image
        assertNull(instance.getDimension(new Identifier("bogus")));
    }

    @Test
    public void testGetImageInputStreamWithZeroTtl() {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.getImageInputStream(ops));
    }

    @Test
    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bees"));

        OutputStream bc = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), bc);
        bc.close();
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        // existing, non-expired image
        assertNotNull(instance.getImageInputStream(ops));

        // existing, expired image
        ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNull(instance.getImageInputStream(ops));

        // nonexistent image
        ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bogus"));
        assertNull(instance.getImageInputStream(ops));
    }

    @Test
    public void testGetImageOutputStream() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.getImageOutputStream(ops));
    }

    @Test
    public void testOldestValidDate() {
        // ttl = 0
        assertEquals(new Date(Long.MIN_VALUE), instance.oldestValidDate());
        // ttl = 50
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 50);
        long expectedTime = Date.from(Instant.now().minus(Duration.ofSeconds(50))).getTime();
        long actualTime = instance.oldestValidDate().getTime();
        assertTrue(Math.abs(actualTime - expectedTime) < 100);
    }

    @Test
    public void testPutDimension() throws CacheException {
        Identifier identifier = new Identifier("birds");
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

}
