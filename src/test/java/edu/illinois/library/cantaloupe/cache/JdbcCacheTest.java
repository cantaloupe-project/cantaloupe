package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.*;

public class JdbcCacheTest extends BaseTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private JdbcCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        // use an in-memory H2 database
        config.setProperty(JdbcCache.JDBC_URL_CONFIG_KEY, "jdbc:h2:mem:test");
        config.setProperty(JdbcCache.USER_CONFIG_KEY, "sa");
        config.setProperty(JdbcCache.PASSWORD_CONFIG_KEY, "");
        config.setProperty(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY, "deriv");
        config.setProperty(JdbcCache.INFO_TABLE_CONFIG_KEY, "info");
        config.setProperty(Cache.TTL_CONFIG_KEY, 0);

        try (Connection connection = JdbcCache.getConnection()) {
            createTables(connection);
            instance = new JdbcCache();
            seed(connection);
        }
    }

    @After
    public void tearDown() throws CacheException {
        instance.purge();
    }

    private void createTables(Connection connection) throws Exception {
        // derivative image table
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "%s VARCHAR(4096) NOT NULL, " +
                "%s BLOB, " +
                "%s DATETIME);",
                JdbcCache.getDerivativeImageTableName(),
                JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                JdbcCache.DERIVATIVE_IMAGE_TABLE_IMAGE_COLUMN,
                JdbcCache.DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();

        // info table
        sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "%s VARCHAR(4096) NOT NULL, " +
                        "%s VARCHAR(8192) NOT NULL, " +
                        "%s DATETIME);",
                JdbcCache.getInfoTableName(),
                JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                JdbcCache.INFO_TABLE_INFO_COLUMN,
                JdbcCache.INFO_TABLE_LAST_ACCESSED_COLUMN);
        statement = connection.prepareStatement(sql);
        statement.execute();
    }

    private void seed(Connection connection) throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();

        // persist some derivative images
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));

        OutputStream os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), os);
        os.close();

        Identifier identifier = new Identifier("dogs");
        Crop crop = new Crop();
        crop.setX(50f);
        crop.setY(50f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale(0.9f);
        Rotate rotate = new Rotate();
        Format format = Format.JPG;
        ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), os);
        os.close();

        identifier = new Identifier("bunnies");
        crop = new Crop();
        crop.setX(10f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(90f);
        scale = new Scale(40, null, Scale.Mode.ASPECT_FIT_WIDTH);
        rotate = new Rotate(15);
        format = Format.PNG;
        ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), os);
        os.close();

        // persist some infos corresponding to the above images
        instance.putImageInfo(new Identifier("cats"), new ImageInfo(50, 40));
        instance.putImageInfo(new Identifier("dogs"), new ImageInfo(500, 300));
        instance.putImageInfo(new Identifier("bunnies"), new ImageInfo(350, 240));

        // assert that the data has been seeded
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                config.getString(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY));
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

    /* getImageInfo(Identifier) */

    @Test
    public void testGetImageInfoWithZeroTtl() throws CacheException {
        // existing image
        try {
            ImageInfo actual = instance.getImageInfo(new Identifier("cats"));
            assertEquals(actual, new ImageInfo(50, 40));
        } catch (CacheException e) {
            fail();
        }
        // nonexistent image
        assertNull(instance.getImageInfo(new Identifier("bogus")));
    }

    @Test
    public void testGetImageInfoWithNonZeroTtl() throws Exception {
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bees"));

        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)),
                instance.getImageOutputStream(ops));
        instance.putImageInfo(new Identifier("bees"), new ImageInfo(50, 40));

        // existing, non-expired image
        try {
            ImageInfo actual = instance.getImageInfo(new Identifier("bees"));
            assertEquals(actual, new ImageInfo(50, 40));
        } catch (CacheException e) {
            fail();
        }
        // existing, expired image
        assertNull(instance.getImageInfo(new Identifier("cats")));
        // nonexistent image
        assertNull(instance.getImageInfo(new Identifier("bogus")));
    }

    @Test
    public void testGetImageInfoUpdatesLastAccessedTime() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();

        final Identifier identifier = new Identifier("cats");

        try (Connection connection = JdbcCache.getConnection()) {
            // get the initial last-accessed time
            String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
                    JdbcCache.INFO_TABLE_LAST_ACCESSED_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY),
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, identifier.toString());
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time1 = resultSet.getTimestamp(1);

            // run the clock
            Thread.sleep(10);

            // update the last-accessed time
            instance.getImageInfo(identifier);

            // get the new last-accessed time
            resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time2 = resultSet.getTimestamp(1);

            // compare them
            assertTrue(time2.after(time1));
        }
    }

    /* getImageInputStream(OperationList) */

    @Test
    public void testGetImageInputStreamWithOpListWithZeroTtl()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.getImageInputStream(ops));
    }

    @Test
    public void testGetImageInputStreamWithOpListWithNonzeroTtl()
            throws Exception {
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bees"));

        OutputStream bc = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), bc);
        bc.close();

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
    public void testGetImageInputStreamWithOpListUpdatesLastAccessedTime()
            throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();

        final OperationList opList = TestUtil.newOperationList();
        opList.setIdentifier(new Identifier("cats"));

        try (Connection connection = JdbcCache.getConnection()) {
            // get the initial last-accessed time
            String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN,
                    config.getString(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY),
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, opList.toString());
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time1 = resultSet.getTimestamp(1);

            // run the clock
            Thread.sleep(10);

            // update the last-accessed time
            instance.getImageInputStream(opList);

            // get the new last-accessed time
            resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time2 = resultSet.getTimestamp(1);

            // compare them
            assertTrue(time2.after(time1));
        }
    }

    /* getImageOutputStream(OperationList) */

    @Test
    public void testGetImageOutputStreamWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.getImageOutputStream(ops));
    }

    /* oldestValidDate() */

    @Test
    public void testOldestValidDate() {
        // ttl = 0
        assertEquals(new Date(Long.MIN_VALUE), instance.oldestValidDate());
        // ttl = 50
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 50);
        long expectedTime = Date.from(Instant.now().minus(Duration.ofSeconds(50))).getTime();
        long actualTime = instance.oldestValidDate().getTime();
        assertTrue(Math.abs(actualTime - expectedTime) < 100);
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();

        instance.purge();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the derivative images were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(0, resultSet.getInt("count"));

            // assert that the infos were purged
            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(0, resultSet.getInt("count"));
        }
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        instance.purge(ops);

        Configuration config = ConfigurationFactory.getInstance();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the derivative image was purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));

            // assert that the info was NOT purged
            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(3, resultSet.getInt("count"));
        }
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Cache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities...
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        // ...derivative image
        OutputStream os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getImage(IMAGE)), os);
        os.close();
        // ...info
        instance.putImageInfo(new Identifier("bees"), new ImageInfo(50, 40));

        instance.purgeExpired();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that only the expired derivative images were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(1, resultSet.getInt("count"));

            // assert that only the expired infos were purged
            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(1, resultSet.getInt("count"));
        }
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();

        Identifier id1 = new Identifier("cats");
        instance.purge(id1);

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the derivative images were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(JdbcCache.DERIVATIVE_IMAGE_TABLE_CONFIG_KEY));
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

    /* putImageInfo(Identifier, ImageInfo) */

    @Test
    public void testPutImageInfo() throws CacheException {
        Identifier identifier = new Identifier("birds");
        ImageInfo info = new ImageInfo(52, 52);
        instance.putImageInfo(identifier, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testPutImageInfoSetsLastAccessedTime() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();

        Identifier identifier = new Identifier("birds");
        ImageInfo info = new ImageInfo(52, 52);
        instance.putImageInfo(identifier, info);

        try (Connection connection = JdbcCache.getConnection()) {
            // get the initial last-accessed time
            String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
                    JdbcCache.INFO_TABLE_LAST_ACCESSED_COLUMN,
                    config.getString(JdbcCache.INFO_TABLE_CONFIG_KEY),
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, identifier.toString());
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertNotNull(resultSet.getTimestamp(1));
        }
    }

}
