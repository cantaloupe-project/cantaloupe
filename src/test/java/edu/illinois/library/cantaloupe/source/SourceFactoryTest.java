package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SourceFactoryTest extends BaseTest {

    private SourceFactory instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new SourceFactory();
    }

    @Test
    public void getAllSources() {
        assertEquals(6, SourceFactory.getAllSources().size());
    }

    @Test
    public void newSourceWithValidStaticResolverSimpleClassName()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC,
                HttpSource.class.getSimpleName());

        Identifier identifier = new Identifier("cats");
        Source source = instance.newSource(identifier, null);
        assertTrue(source instanceof HttpSource);
    }

    @Test
    public void newSourceWithValidStaticResolverFullClassName()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, HttpSource.class.getName());

        Identifier identifier = new Identifier("cats");
        Source source = instance.newSource(identifier, null);

        assertTrue(source instanceof HttpSource);
    }

    @Test(expected = ClassNotFoundException.class)
    public void newSourceWithInvalidStaticResolver() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, "BogusSource");

        Identifier identifier = new Identifier("cats");
        instance.newSource(identifier, null);
    }

    @Test
    public void newSourceUsingDelegateScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.SOURCE_DELEGATE, true);

        Identifier identifier = new Identifier("http");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        Source source = instance.newSource(identifier, proxy);
        assertTrue(source instanceof HttpSource);

        identifier = new Identifier("anythingelse");
        context = new RequestContext();
        context.setIdentifier(identifier);
        service = DelegateProxyService.getInstance();
        proxy = service.newDelegateProxy(context);

        source = instance.newSource(identifier, proxy);
        assertTrue(source instanceof FilesystemSource);
    }

    @Test
    public void getSelectionStrategy() {
        Configuration config = Configuration.getInstance();

        config.setProperty(Key.SOURCE_DELEGATE, "false");
        assertEquals(SourceFactory.SelectionStrategy.STATIC,
                instance.getSelectionStrategy());

        config.setProperty(Key.SOURCE_DELEGATE, "true");
        assertEquals(SourceFactory.SelectionStrategy.DELEGATE_SCRIPT,
                instance.getSelectionStrategy());
    }

}
