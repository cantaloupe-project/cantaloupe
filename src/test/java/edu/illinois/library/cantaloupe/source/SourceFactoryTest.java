package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SourceFactoryTest extends BaseTest {

    private SourceFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new SourceFactory();
    }

    @Test
    void getAllSources() {
        assertEquals(5, SourceFactory.getAllSources().size());
    }

    /* newSource(String) */

    @Test
    void testNewSourceWithStringWithUnqualifiedName() throws Exception {
        assertTrue(instance.newSource(FilesystemSource.class.getSimpleName()) instanceof FilesystemSource);
    }

    @Test
    void testNewSourceWithStringWithNonExistingUnqualifiedName() {
        assertThrows(ClassNotFoundException.class, () ->
                instance.newSource("Bogus"));
    }

    @Test
    void testNewSourceWithStringWithQualifiedName() throws Exception {
        assertTrue(instance.newSource(FilesystemSource.class.getName()) instanceof FilesystemSource);
    }

    @Test
    void testNewSourceWithStringWithNonExistingQualifiedName() {
        assertThrows(ClassNotFoundException.class, () ->
                instance.newSource(SourceFactory.class.getPackage().getName() + ".Bogus"));
    }

    /* newSource(Identifier, DelegateProxy) */

    @Test
    void newSourceWithIdentifierWithValidStaticSourceSimpleClassName() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC,
                HttpSource.class.getSimpleName());

        Identifier identifier = new Identifier("cats");
        Source source = instance.newSource(identifier, null);
        assertTrue(source instanceof HttpSource);
    }

    @Test
    void newSourceWithIdentifierWithValidStaticSourceFullClassName()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, HttpSource.class.getName());

        Identifier identifier = new Identifier("cats");
        Source source = instance.newSource(identifier, null);

        assertTrue(source instanceof HttpSource);
    }

    @Test
    void newSourceWithIdentifierWithInvalidStaticSource() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, "BogusSource");

        Identifier identifier = new Identifier("cats");
        assertThrows(ClassNotFoundException.class,
                () -> instance.newSource(identifier, null));
    }

    @Test
    void newSourceWithIdentifierUsingDelegateScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_DELEGATE, true);

        Identifier identifier = new Identifier("http");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);

        Source source = instance.newSource(identifier, proxy);
        assertTrue(source instanceof HttpSource);

        identifier = new Identifier("anythingelse");
        proxy.getRequestContext().setIdentifier(identifier);

        source = instance.newSource(identifier, proxy);
        assertTrue(source instanceof FilesystemSource);
    }

    @Test
    void getSelectionStrategy() {
        Configuration config = Configuration.getInstance();

        config.setProperty(Key.SOURCE_DELEGATE, "false");
        assertEquals(SourceFactory.SelectionStrategy.STATIC,
                instance.getSelectionStrategy());

        config.setProperty(Key.SOURCE_DELEGATE, "true");
        assertEquals(SourceFactory.SelectionStrategy.DELEGATE_SCRIPT,
                instance.getSelectionStrategy());
    }

}
