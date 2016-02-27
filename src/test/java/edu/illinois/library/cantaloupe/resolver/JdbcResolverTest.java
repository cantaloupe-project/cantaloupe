package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.Assert.*;

public class JdbcResolverTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private JdbcResolver instance;

    @Before
    public void setUp() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        // use an in-memory H2 database
        config.setProperty(JdbcResolver.JDBC_URL_CONFIG_KEY, "jdbc:h2:mem:test");
        config.setProperty(JdbcResolver.USER_CONFIG_KEY, "sa");
        config.setProperty(JdbcResolver.PASSWORD_CONFIG_KEY, "");
        config.setProperty(JdbcResolver.IDENTIFIER_FUNCTION_CONFIG_KEY,
                "function getDatabaseIdentifier(url_identifier) { return url_identifier; }");
        config.setProperty(JdbcResolver.LOOKUP_SQL_CONFIG_KEY,
                "SELECT image FROM items WHERE filename = ?");
        config.setProperty(JdbcResolver.MEDIA_TYPE_FUNCTION_CONFIG_KEY,
                "function getMediaType(identifier) { return \"SELECT media_type FROM items WHERE filename = ?\" }");
        Application.setConfiguration(config);

        try (Connection conn = JdbcResolver.getConnection()) {
            // create the table
            String sql = "CREATE TABLE IF NOT EXISTS items (" +
                    "filename VARCHAR(255)," +
                    "media_type VARCHAR(255)," +
                    "image BLOB);";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.execute();

            // insert some images
            sql = "INSERT INTO items (filename, media_type, image) VALUES (?, ?, ?)";
            statement = conn.prepareStatement(sql);
            statement.setString(1, "jpg.jpg");
            statement.setString(2, "image/jpeg");
            statement.setBinaryStream(3,
                    new FileInputStream(TestUtil.getImage(IDENTIFIER.toString())));
            statement.executeUpdate();

            instance = new JdbcResolver();
            instance.setIdentifier(IDENTIFIER);
        }
    }

    @Test
    public void tearDown() throws Exception {
        try (Connection conn = JdbcResolver.getConnection()) {
            String sql = "DROP TABLE items;";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.execute();
        }
    }

    @Test
    public void testGetStreamSource() throws IOException {
        // present, readable image
        try {
            instance.setIdentifier(new Identifier("jpg.jpg"));
            assertNotNull(instance.getStreamSource());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.getStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetSourceFormat() throws IOException {
        // JdbcResolver.function.media_type returns SQL
        instance.setIdentifier(new Identifier("jpg.jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
        instance.setIdentifier(new Identifier("bogus"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());

        // JdbcResolver.function.media_type returns a media type
        Application.getConfiguration().setProperty(
                "JdbcResolver.function.media_type",
                "function getMediaType(identifier) { return (identifier == 'bogus') ? null : 'image/jpeg'; }");
        instance.setIdentifier(new Identifier("jpg.jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
        instance.setIdentifier(new Identifier("bogus"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());

        // JdbcResolver.function.media_type is undefined
        Application.getConfiguration().setProperty(
                "JdbcResolver.function.media_type", null);
        instance.setIdentifier(new Identifier("jpg.jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
        instance.setIdentifier(new Identifier("bogus"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    @Test
    public void testExecuteGetDatabaseIdentifier() throws Exception {
        instance.setIdentifier(new Identifier("cats.jpg"));
        String result = instance.executeGetDatabaseIdentifier();
        assertEquals("cats.jpg", result);
    }

    @Test
    public void testExecuteGetMediaType() throws Exception {
        instance.setIdentifier(new Identifier("cats.jpg"));
        String result = instance.executeGetMediaType();
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

}
