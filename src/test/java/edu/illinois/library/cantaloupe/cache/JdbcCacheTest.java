package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
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
import org.junit.Ignore;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

public class JdbcCacheTest extends AbstractCacheTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private JdbcCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        configure();

        try (Connection connection = JdbcCache.getConnection()) {
            createTables(connection);
            instance = newInstance();
            seed(connection);
        }
    }

    @After
    public void tearDown() throws Exception {
        instance.purge();
    }

    @Override
    JdbcCache newInstance() {
        return new JdbcCache();
    }

    private void configure() {
        Configuration config = Configuration.getInstance();
        // use an in-memory H2 database
        config.setProperty(Key.JDBCCACHE_JDBC_URL, "jdbc:h2:mem:test");
        config.setProperty(Key.JDBCCACHE_USER, "sa");
        config.setProperty(Key.JDBCCACHE_PASSWORD, "");
        config.setProperty(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE, "deriv");
        config.setProperty(Key.JDBCCACHE_INFO_TABLE, "info");
    }

    private void createTables(Connection connection) throws SQLException {
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
            Files.copy(TestUtil.getImage(IMAGE), os);
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
            Files.copy(TestUtil.getImage(IMAGE), os);
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
            Files.copy(TestUtil.getImage(IMAGE), os);
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

    /* earliestValidDate() */

    @Test
    public void testEarliestValidDate() {
        final Configuration config = Configuration.getInstance();

        // ttl = 0
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 0);
        assertEquals(new Timestamp(0), instance.earliestValidDate());

        // ttl = 50
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 50);
        Instant expected = Instant.now().minus(Duration.ofSeconds(50)).
                truncatedTo(ChronoUnit.SECONDS);
        Instant actual = instance.earliestValidDate().toInstant().
                truncatedTo(ChronoUnit.SECONDS);
        assertEquals(expected, actual);
    }

    /* getImageInfo(Identifier) */

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

            // this should cause the last-accessed time to update asynchronously
            instance.getImageInfo(identifier);

            Thread.sleep(100);

            // get the new last-accessed time
            resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time2 = resultSet.getTimestamp(1);

            // compare them
            assertTrue(time2.after(time1));
        }
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Ignore // TODO: why does this fail?
    @Override
    @Test
    public void testNewDerivativeImageInputStreamWithZeroTTL() {}

    @Ignore // TODO: why does this fail?
    @Override
    @Test
    public void testNewDerivativeImageInputStreamWithNonzeroTTL() {}

    @Test
    public void testNewDerivativeImageInputStreamUpdatesLastAccessedTime()
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

            // Access the image to update the last-accessed time (this will
            // happen asynchronously)
            instance.newDerivativeImageInputStream(opList).close();

            // wait for it to happen
            Thread.sleep(100);

            // get the new last-accessed time
            resultSet = statement.executeQuery();
            resultSet.next();
            Timestamp time2 = resultSet.getTimestamp(1);

            // compare them
            assertTrue(time2.after(time1));
        }
    }

    /* newDerivativeImageOutputStream() */

    @Ignore // TODO: why does this fail?
    @Override
    @Test
    public void testNewDerivativeImageOutputStream() {}

    /* put(Identifier, Info) */

    @Test
    public void testPutSetsLastAccessedTime() throws Exception {
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
