package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotation;
import edu.illinois.library.cantaloupe.image.Scale;
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
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        OutputStream os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();

        identifier = new Identifier("dogs");
        crop = new Crop();
        crop.setX(50f);
        crop.setY(50f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        scale = new Scale();
        scale.setPercent(0.9f);
        rotation = new Rotation();
        filter = Filter.NONE;
        format = OutputFormat.JPG;
        ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();

        identifier = new Identifier("bunnies");
        crop = new Crop();
        crop.setX(10f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(90f);
        scale = new Scale();
        scale.setWidth(40);
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        rotation = new Rotation(15);
        filter = Filter.NONE;
        format = OutputFormat.PNG;
        ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();

        // persist some corresponding dimensions
        instance.putDimension(new Identifier("cats"), new Dimension(50, 40));
        instance.putDimension(new Identifier("dogs"), new Dimension(500, 300));
        instance.putDimension(new Identifier("bunnies"), new Dimension(350, 240));

        // assert that the data has been seeded
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s;",
                JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
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
                JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
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

    public void testFlushWithOperations() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        instance.flush(ops);

        Configuration config = Application.getConfiguration();

        // assert that the image and info were flushed
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
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
        Identifier identifier = new Identifier("bees");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        OutputStream os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        instance.flushExpired();

        // assert that only the expired images and infos were flushed
        Configuration config = Application.getConfiguration();
        String sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
                config.getString(JdbcCache.IMAGE_TABLE_CONFIG_KEY));
        PreparedStatement statement = JdbcCache.getConnection().prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        assertEquals(1, resultSet.getInt("count"));

        sql = String.format("SELECT COUNT(%s) AS count FROM %s",
                JdbcCache.IMAGE_TABLE_OPERATIONS_COLUMN,
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
        Identifier identifier = new Identifier("bees");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")),
                instance.getImageOutputStream(ops));
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
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);
        assertNotNull(instance.getImageInputStream(ops));
    }

    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Application.getConfiguration().setProperty(JdbcCache.TTL_CONFIG_KEY, 1);

        // wait for the seed data to invalidate
        Thread.sleep(1500);

        // add some fresh entities
        Identifier identifier = new Identifier("bees");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);
        OutputStream os = instance.getImageOutputStream(ops);
        IOUtils.copy(new FileInputStream(TestUtil.getFixture("jpg")), os);
        os.close();
        instance.putDimension(new Identifier("bees"), new Dimension(50, 40));

        // existing, non-expired image
        assertNotNull(instance.getImageInputStream(ops));
        // existing, expired image
        identifier = new Identifier("cats");
        crop = new Crop();
        scale = new Scale();
        rotation = new Rotation();
        filter = Filter.NONE;
        format = OutputFormat.JPG;
        ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);
        assertNull(instance.getImageInputStream(ops));
        // nonexistent image
        identifier = new Identifier("bogus");
        crop = new Crop();
        scale = new Scale();
        rotation = new Rotation();
        filter = Filter.NONE;
        format = OutputFormat.JPG;
        ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);
        assertNull(instance.getImageInputStream(ops));
    }

    public void testGetImageOutputStream() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        Scale scale = new Scale();
        Rotation rotation = new Rotation();
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);
        assertNotNull(instance.getImageOutputStream(ops));
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
