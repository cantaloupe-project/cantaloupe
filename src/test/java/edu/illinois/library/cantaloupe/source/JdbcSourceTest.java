package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.script.DisabledException;
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
import java.sql.Types;
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
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates-truffle.rb").toString());

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
                sql = "INSERT INTO items (filename, media_type, image) VALUES (?, ?, ?);";

                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.setString(1, filename);
                    if (IMAGE_WITHOUT_EXTENSION_OR_MEDIA_TYPE.equals(filename) ||
                            IMAGE_WITH_EXTENSION_WITHOUT_MEDIA_TYPE.equals(filename) ||
                            IMAGE_WITH_INCORRECT_EXTENSION_WITHOUT_MEDIA_TYPE.equals(filename)) {
                        statement.setNull(2, Types.VARCHAR);
                    } else {
                        statement.setString(2, "image/jpeg");
                    }
                    try (InputStream is = Files.newInputStream(TestUtil.getImage("jpg"))) {
                        statement.setBinaryStream(3, is);
                    }
                    statement.executeUpdate();
                }
            }
        }
    }

    @Override
    JdbcSource newInstance() {
        JdbcSource instance = new JdbcSource();
        try {
            Identifier identifier = new Identifier(IMAGE_WITH_EXTENSION_WITH_MEDIA_TYPE);
            RequestContext context = new RequestContext();
            context.setIdentifier(identifier);
            DelegateProxyService service = DelegateProxyService.getInstance();
            DelegateProxy proxy = service.newDelegateProxy(context);
            instance.setDelegateProxy(proxy);
            instance.setIdentifier(identifier);
        } catch (DisabledException ignore) {}

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

    /* checkAccess() */

    @Override
    @Test
    void testCheckAccessUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        Identifier identifier = new Identifier("bogus");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, instance::checkAccess);
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorHasNext() throws Exception {
        final Identifier identifier =
                new Identifier(IMAGE_WITH_EXTENSION_WITH_MEDIA_TYPE);
        JdbcSource source = newInstance();
        source.setIdentifier(identifier);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
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
    void testGetFormatIteratorNext() throws Exception {
        final Identifier identifier =
                new Identifier(IMAGE_WITH_INCORRECT_EXTENSION_WITHOUT_MEDIA_TYPE);
        JdbcSource source = newInstance();
        source.setIdentifier(identifier);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        source.setDelegateProxy(proxy);
        source.setIdentifier(identifier);

        JdbcSource.FormatIterator<Format> it = source.getFormatIterator();
        assertEquals(Format.PNG, it.next()); // identifier extension
        assertEquals(Format.UNKNOWN, it.next()); // media type column
        assertEquals(Format.JPG, it.next()); // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getDatabaseIdentifier() */

    @Test
    void testGetDatabaseIdentifier() throws Exception {
        Identifier identifier = new Identifier("cats.jpg");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        String result = instance.getDatabaseIdentifier();
        assertEquals("cats.jpg", result);
    }

    /* getLookupSQL() */

    @Test
    void testGetLookupSQL() throws Exception {
        Identifier identifier = new Identifier("cats.jpg");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
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

}
