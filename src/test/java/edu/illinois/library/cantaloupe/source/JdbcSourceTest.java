package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcSourceTest extends AbstractSourceTest {

    private static final String IMAGE_WITH_EXTENSION_WITH_MEDIA_TYPE              = "jpg-1.jpg";
    private static final String IMAGE_WITHOUT_EXTENSION_WITH_MEDIA_TYPE           = "jpg-2";
    private static final String IMAGE_WITH_EXTENSION_WITHOUT_MEDIA_TYPE           = "jpg-3.jpg";
    private static final String IMAGE_WITH_INCORRECT_EXTENSION_WITHOUT_MEDIA_TYPE = "jpg.png";
    private static final String IMAGE_WITHOUT_EXTENSION_OR_MEDIA_TYPE             = "jpg-4";

    private JdbcSource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        // Use an in-memory H2 database.
        config.setProperty(Key.JDBCSOURCE_JDBC_URL, "jdbc:h2:mem:test");
        config.setProperty(Key.JDBCSOURCE_USER, "sa");
        config.setProperty(Key.JDBCSOURCE_PASSWORD, "");

        initializeEndpoint();

        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        destroyEndpoint();
    }

    @Override
    void destroyEndpoint() throws Exception {
        try (Connection conn = JdbcSource.getConnection()) {
            String sql = "DROP TABLE items;";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.execute();
            } catch (SQLException e) {
                if (!e.getMessage().contains("not found")) {
                    throw e;
                }
            }
        }
    }

    @Override
    void initializeEndpoint() throws Exception {
        try (Connection conn = JdbcSource.getConnection()) {
            // create the table
            String sql = "CREATE TABLE IF NOT EXISTS items (" +
                    "filename VARCHAR(255)," +
                    "media_type VARCHAR(255)," +
                    "last_modified TIMESTAMP," +
                    "image BLOB);";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.execute();
            }

            // insert some images
            for (String filename : new String[] {
                    IMAGE_WITH_EXTENSION_WITH_MEDIA_TYPE,
                    IMAGE_WITHOUT_EXTENSION_WITH_MEDIA_TYPE,
                    IMAGE_WITH_EXTENSION_WITHOUT_MEDIA_TYPE,
                    IMAGE_WITH_INCORRECT_EXTENSION_WITHOUT_MEDIA_TYPE,
                    IMAGE_WITHOUT_EXTENSION_OR_MEDIA_TYPE}) {
                sql = "INSERT INTO items (filename, last_modified, media_type, image) " +
                        "VALUES (?, ?, ?, ?);";

                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.setString(1, filename);
                    statement.setTimestamp(2, Timestamp.from(Instant.now()));
                    if (IMAGE_WITHOUT_EXTENSION_OR_MEDIA_TYPE.equals(filename) ||
                            IMAGE_WITH_EXTENSION_WITHOUT_MEDIA_TYPE.equals(filename) ||
                            IMAGE_WITH_INCORRECT_EXTENSION_WITHOUT_MEDIA_TYPE.equals(filename)) {
                        statement.setNull(3, Types.VARCHAR);
                    } else {
                        statement.setString(3, "image/jpeg");
                    }
                    try (InputStream is = Files.newInputStream(TestUtil.getImage("jpg"))) {
                        statement.setBinaryStream(4, is);
                    }
                    statement.executeUpdate();
                }
            }
        }
    }

    @Override
    JdbcSource newInstance() {
        JdbcSource instance = new JdbcSource();
        Identifier identifier = new Identifier(IMAGE_WITH_EXTENSION_WITH_MEDIA_TYPE);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        useScriptLookupStrategy();
    }

    @Override
    void useScriptLookupStrategy() {
        // This source is always using ScriptLookupStrategy.
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorHasNext() {
        final Identifier identifier =
                new Identifier(IMAGE_WITH_EXTENSION_WITH_MEDIA_TYPE);
        JdbcSource source = newInstance();
        source.setIdentifier(identifier);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        source.setDelegateProxy(proxy);
        source.setIdentifier(identifier);

        JdbcSource.FormatIterator<Format> it = source.getFormatIterator();
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // media type column
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void testGetFormatIteratorNext() {
        final Identifier identifier =
                new Identifier(IMAGE_WITH_INCORRECT_EXTENSION_WITHOUT_MEDIA_TYPE);
        JdbcSource source = newInstance();
        source.setIdentifier(identifier);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        source.setDelegateProxy(proxy);
        source.setIdentifier(identifier);

        JdbcSource.FormatIterator<Format> it = source.getFormatIterator();
        assertEquals(Format.get("png"), it.next()); // identifier extension
        assertEquals(Format.UNKNOWN, it.next()); // media type column
        assertEquals(Format.get("jpg"), it.next()); // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getDatabaseIdentifier() */

    @Test
    void testGetDatabaseIdentifier() throws Exception {
        Identifier identifier = new Identifier("cats.jpg");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        String result = instance.getDatabaseIdentifier();
        assertEquals("cats.jpg", result);
    }

    /* getLookupSQL() */

    @Test
    void testGetLookupSQL() throws Exception {
        Identifier identifier = new Identifier("cats.jpg");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        String result = instance.getLookupSQL();
        assertEquals("SELECT image FROM items WHERE filename = ?", result);
    }

    /* getMediaType() */

    @Test
    void testGetMediaType() throws Exception {
        instance.setIdentifier(new Identifier("cats.jpg"));
        String result = instance.getMediaType();
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryWithPresentImage() throws Exception {
        assertNotNull(instance.newStreamFactory());
    }

    /* stat() */

    @Override
    @Test
    void testStatUsingBasicLookupStrategyWithMissingImage() {
        Identifier identifier = new Identifier("bogus");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, instance::stat);
    }

}
