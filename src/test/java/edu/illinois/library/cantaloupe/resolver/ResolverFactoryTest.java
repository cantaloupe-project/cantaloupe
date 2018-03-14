package edu.illinois.library.cantaloupe.resolver;

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

public class ResolverFactoryTest extends BaseTest {

    private ResolverFactory instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ResolverFactory();
    }

    @Test
    public void getAllResolvers() {
        assertEquals(5, ResolverFactory.getAllResolvers().size());
    }

    @Test
    public void newResolverWithValidStaticResolverSimpleClassName()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC,
                HttpResolver.class.getSimpleName());

        Identifier identifier = new Identifier("cats");
        Resolver resolver = instance.newResolver(identifier, null);
        assertTrue(resolver instanceof HttpResolver);
    }

    @Test
    public void newResolverWithValidStaticResolverFullClassName()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, HttpResolver.class.getName());

        Identifier identifier = new Identifier("cats");
        Resolver resolver = instance.newResolver(identifier, null);

        assertTrue(resolver instanceof HttpResolver);
    }

    @Test(expected = ClassNotFoundException.class)
    public void newResolverWithInvalidStaticResolver() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "BogusResolver");

        Identifier identifier = new Identifier("cats");
        instance.newResolver(identifier, null);
    }

    @Test
    public void newResolverUsingDelegateScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.RESOLVER_DELEGATE, true);

        Identifier identifier = new Identifier("http");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        Resolver resolver = instance.newResolver(identifier, proxy);
        assertTrue(resolver instanceof HttpResolver);

        identifier = new Identifier("anythingelse");
        context = new RequestContext();
        context.setIdentifier(identifier);
        service = DelegateProxyService.getInstance();
        proxy = service.newDelegateProxy(context);

        resolver = instance.newResolver(identifier, proxy);
        assertTrue(resolver instanceof FilesystemResolver);
    }

    @Test
    public void getSelectionStrategy() {
        Configuration config = Configuration.getInstance();

        config.setProperty(Key.RESOLVER_DELEGATE, "false");
        assertEquals(ResolverFactory.SelectionStrategy.STATIC,
                instance.getSelectionStrategy());

        config.setProperty(Key.RESOLVER_DELEGATE, "true");
        assertEquals(ResolverFactory.SelectionStrategy.DELEGATE_SCRIPT,
                instance.getSelectionStrategy());
    }

}
