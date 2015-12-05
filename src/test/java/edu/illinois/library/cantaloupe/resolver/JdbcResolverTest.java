package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class JdbcResolverTest extends CantaloupeTestCase {

    private JdbcResolver instance;

    public void setUp() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        // use an in-memory H2 database
        config.setProperty("JdbcResolver.connection_string", "jdbc:h2:mem:test");
        config.setProperty("JdbcResolver.user", "sa");
        config.setProperty("JdbcResolver.password", "");
        config.setProperty("JdbcResolver.function.identifier",
                "function getDatabaseIdentifier(url_identifier) { return url_identifier; }");
        config.setProperty("JdbcResolver.lookup_sql",
                "SELECT image FROM items WHERE filename = ?");
        config.setProperty("JdbcResolver.function.media_type",
                "function getMediaType(identifier) { return \"SELECT media_type FROM items WHERE filename = ?\" }");
        Application.setConfiguration(config);

        try (Connection conn = JdbcResolver.getConnection()) {
            // create the table
            String sql = "CREATE TABLE items (" +
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
                    new FileInputStream(TestUtil.getFixture("jpg")));
            statement.executeUpdate();

            instance = new JdbcResolver();
        }
    }

    public void tearDown() throws Exception {
        try (Connection conn = JdbcResolver.getConnection()) {
            String sql = "DROP TABLE items;";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.execute();
        }
    }

    public void testGetInputStream() {
        // present image
        try {
            assertNotNull(instance.getInputStream(new Identifier("jpg.jpg")));
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.getInputStream(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    public void testGetSourceFormat() throws IOException {
        // JdbcResolver.function.media_type returns SQL
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("jpg.jpg")));
        assertEquals(SourceFormat.UNKNOWN,
                instance.getSourceFormat(new Identifier("bogus")));

        // JdbcResolver.function.media_type returns a media type
        Application.getConfiguration().setProperty(
                "JdbcResolver.function.media_type",
                "function getMediaType(identifier) { return (identifier == 'bogus') ? null : 'image/jpeg'; }");
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("jpg.jpg")));
        assertEquals(SourceFormat.UNKNOWN,
                instance.getSourceFormat(new Identifier("bogus")));

        // JdbcResolver.function.media_type is undefined
        Application.getConfiguration().setProperty(
                "JdbcResolver.function.media_type", null);
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("jpg.jpg")));
        assertEquals(SourceFormat.UNKNOWN,
                instance.getSourceFormat(new Identifier("bogus")));
    }

    public void testExecuteGetDatabaseIdentifier() throws Exception {
        String result = instance.
                executeGetDatabaseIdentifier(new Identifier("cats.jpg"));
        assertEquals("cats.jpg", result);
    }

    public void testExecuteGetMediaType() throws Exception {
        String result = instance.executeGetMediaType(new Identifier("cats.jpg"));
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

}
