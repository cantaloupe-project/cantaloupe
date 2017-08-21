package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.file.Files;
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

        Configuration config = Configuration.getInstance();
        // use an in-memory H2 database
        config.setProperty(Key.JDBCCACHE_JDBC_URL, "jdbc:h2:mem:test");
        config.setProperty(Key.JDBCCACHE_USER, "sa");
        config.setProperty(Key.JDBCCACHE_PASSWORD, "");
        config.setProperty(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE, "deriv");
        config.setProperty(Key.JDBCCACHE_INFO_TABLE, "info");
        config.setProperty(Key.CACHE_SERVER_TTL, 0);

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
        final Configuration config = Configuration.getInstance();

        // persist some derivative images
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));

        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE).toPath(), os);
        }

        Identifier identifier = new Identifier("dogs");
        Crop crop = new Crop();
        crop.setX(50f);
        crop.setY(50f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale(0.9f);
        Rotate rotate = new Rotate();
        Format format = Format.JPG;
        ops = new OperationList(identifier, format, crop, scale, rotate);

        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE).toPath(), os);
        }

        identifier = new Identifier("bunnies");
        crop = new Crop();
        crop.setX(10f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(90f);
        scale = new Scale(40, null, Scale.Mode.ASPECT_FIT_WIDTH);
        rotate = new Rotate(15);
        format = Format.PNG;
        ops = new OperationList(identifier, format, crop, scale, rotate);

        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE).toPath(), os);
        }

        // persist some infos corresponding to the above images
        instance.put(new Identifier("cats"), new Info(50, 40));
        instance.put(new Identifier("dogs"), new Info(500, 300));
        instance.put(new Identifier("bunnies"), new Info(350, 240));

        // assert that the data has been seeded
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE));
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            assertEquals(3, resultSet.getInt("count"));
        } else {
            fail();
        }

        sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                config.getString(Key.JDBCCACHE_INFO_TABLE));
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
            Info actual = instance.getImageInfo(new Identifier("cats"));
            assertEquals(actual, new Info(50, 40));
        } catch (CacheException e) {
            fail();
        }
        // nonexistent image
        assertNull(instance.getImageInfo(new Identifier("bogus")));
    }

    @Test
    public void testGetImageInfoWithNonZeroTtl() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bees"));

        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE).toPath(), os);
        }
        instance.put(new Identifier("bees"), new Info(50, 40));

        // existing, non-expired image
        try {
            Info actual = instance.getImageInfo(new Identifier("bees"));
            assertEquals(actual, new Info(50, 40));
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
        final Configuration config = Configuration.getInstance();

        final Identifier identifier = new Identifier("cats");

        try (Connection connection = JdbcCache.getConnection()) {
            // get the initial last-accessed time
            String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
                    JdbcCache.INFO_TABLE_LAST_ACCESSED_COLUMN,
                    config.getString(Key.JDBCCACHE_INFO_TABLE),
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

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStreamWithOpListWithZeroTtl()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.newDerivativeImageInputStream(ops));
    }

    @Test
    public void testNewDerivativeImageInputStreamWithOpListWithNonzeroTtl()
            throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bees"));

        try (OutputStream bc = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE).toPath(), bc);
        }

        // existing, non-expired image
        assertNotNull(instance.newDerivativeImageInputStream(ops));

        // existing, expired image
        ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNull(instance.newDerivativeImageInputStream(ops));

        // nonexistent image
        ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("bogus"));
        assertNull(instance.newDerivativeImageInputStream(ops));
    }

    @Test
    public void testNewDerivativeImageInputStreamWithOpListUpdatesLastAccessedTime()
            throws Exception {
        final Configuration config = Configuration.getInstance();

        final OperationList opList = TestUtil.newOperationList();
        opList.setIdentifier(new Identifier("cats"));

        try (Connection connection = JdbcCache.getConnection()) {
            // get the initial last-accessed time
            String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN,
                    config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE),
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, opList.toString());
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time1 = resultSet.getTimestamp(1);

            // run the clock
            Thread.sleep(10);

            // update the last-accessed time
            instance.newDerivativeImageInputStream(opList);

            // get the new last-accessed time
            resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time2 = resultSet.getTimestamp(1);

            // compare them
            assertTrue(time2.after(time1));
        }
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStreamWithOperationList()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.newDerivativeImageOutputStream(ops));
    }

    /* oldestValidDate() */

    @Test
    public void testOldestValidDate() {
        // ttl = 0
        assertEquals(new Date(Long.MIN_VALUE), instance.oldestValidDate());
        // ttl = 50
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 50);
        long expectedTime = Date.from(Instant.now().minus(Duration.ofSeconds(50))).getTime();
        long actualTime = instance.oldestValidDate().getTime();
        assertTrue(Math.abs(actualTime - expectedTime) < 100);
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        final Configuration config = Configuration.getInstance();

        instance.purge();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the derivative images were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(0, resultSet.getInt("count"));

            // assert that the infos were purged
            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(Key.JDBCCACHE_INFO_TABLE));
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

        Configuration config = Configuration.getInstance();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the derivative image was purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));

            // assert that the info was NOT purged
            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(Key.JDBCCACHE_INFO_TABLE));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(3, resultSet.getInt("count"));
        }
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CACHE_SERVER_TTL, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities...
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        // ...derivative image
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE).toPath(), os);
        }
        // ...info
        instance.put(new Identifier("bees"), new Info(50, 40));

        instance.purgeExpired();

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that only the expired derivative images were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(1, resultSet.getInt("count"));

            // assert that only the expired infos were purged
            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(Key.JDBCCACHE_INFO_TABLE));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(1, resultSet.getInt("count"));
        }
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        Configuration config = Configuration.getInstance();

        Identifier id1 = new Identifier("cats");
        instance.purge(id1);

        try (Connection connection = JdbcCache.getConnection()) {
            // assert that the derivative images were purged
            String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE));
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));

            sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN,
                    config.getString(Key.JDBCCACHE_INFO_TABLE));
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            resultSet.next();
            assertEquals(2, resultSet.getInt("count"));
        }
    }

    /* put(Identifier, Info) */

    @Test
    public void testPutWithImageInfo() throws CacheException {
        Identifier identifier = new Identifier("birds");
        Info info = new Info(52, 52);
        instance.put(identifier, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testPutWithImageInfoSetsLastAccessedTime() throws Exception {
        final Configuration config = Configuration.getInstance();

        Identifier identifier = new Identifier("birds");
        Info info = new Info(52, 52);
        instance.put(identifier, info);

        try (Connection connection = JdbcCache.getConnection()) {
            // get the initial last-accessed time
            String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
                    JdbcCache.INFO_TABLE_LAST_ACCESSED_COLUMN,
                    config.getString(Key.JDBCCACHE_INFO_TABLE),
                    JdbcCache.INFO_TABLE_IDENTIFIER_COLUMN);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, identifier.toString());
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            assertNotNull(resultSet.getTimestamp(1));
        }
    }

}
