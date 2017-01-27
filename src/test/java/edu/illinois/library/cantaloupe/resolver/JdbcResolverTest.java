package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.Assert.*;

public class JdbcResolverTest extends BaseTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private JdbcResolver instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        // Use an in-memory H2 database.
        config.setProperty(JdbcResolver.JDBC_URL_CONFIG_KEY, "jdbc:h2:mem:test");
        config.setProperty(JdbcResolver.USER_CONFIG_KEY, "sa");
        config.setProperty(JdbcResolver.PASSWORD_CONFIG_KEY, "");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

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

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        try (Connection conn = JdbcResolver.getConnection()) {
            String sql = "DROP TABLE items;";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.execute();
        }
    }

    @Test
    public void testGetSourceFormat() throws IOException {
        // get_media_type returns SQL
        instance.setIdentifier(new Identifier("jpg.jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
        instance.setIdentifier(new Identifier("bogus"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    @Test
    public void testNewStreamSource() throws IOException {
        // present, readable image
        try {
            instance.setIdentifier(new Identifier("jpg.jpg"));
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetDatabaseIdentifier() throws Exception {
        instance.setIdentifier(new Identifier("cats.jpg"));
        String result = instance.getDatabaseIdentifier();
        assertEquals("cats.jpg", result);
    }

    @Test
    public void testGetLookupSql() throws Exception {
        String result = instance.getLookupSql();
        assertEquals("SELECT image FROM items WHERE filename = ?", result);
    }

    @Test
    public void testGetMediaType() throws Exception {
        instance.setIdentifier(new Identifier("cats.jpg"));
        String result = instance.getMediaType();
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

}
