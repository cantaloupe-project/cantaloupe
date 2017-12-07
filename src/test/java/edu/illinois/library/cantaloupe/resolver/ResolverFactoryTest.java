package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResolverFactoryTest extends BaseTest {

    private ResolverFactory instance;

    @Before
    public void setUp() {
        instance = new ResolverFactory();
    }

    @Test
    public void getAllResolvers() {
        assertEquals(5, ResolverFactory.getAllResolvers().size());
    }

    @Test
    public void newResolverWithStaticResolver() throws Exception {
        Configuration config = Configuration.getInstance();
        config.clear();

        Identifier identifier = new Identifier("jdbc");

        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        Resolver resolver = instance.newResolver(identifier, new RequestContext());
        assertTrue(resolver instanceof FilesystemResolver);

        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        resolver = instance.newResolver(identifier, new RequestContext());
        assertTrue(resolver instanceof HttpResolver);

        // invalid resolver
        try {
            config.setProperty(Key.RESOLVER_STATIC, "BogusResolver");
            instance.newResolver(identifier, new RequestContext());
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void newResolverUsingDelegateScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(Key.RESOLVER_DELEGATE, true);

        Identifier identifier = new Identifier("http");
        Resolver resolver = instance.newResolver(identifier, new RequestContext());
        assertTrue(resolver instanceof HttpResolver);

        identifier = new Identifier("anythingelse");
        resolver = instance.newResolver(identifier, new RequestContext());
        assertTrue(resolver instanceof FilesystemResolver);
    }

    @Test
    public void getSelectionStrategy() {
        Configuration config = Configuration.getInstance();
        config.clear();

        config.setProperty(Key.RESOLVER_DELEGATE, "false");
        assertEquals(ResolverFactory.SelectionStrategy.STATIC,
                instance.getSelectionStrategy());

        config.setProperty(Key.RESOLVER_DELEGATE, "true");
        assertEquals(ResolverFactory.SelectionStrategy.DELEGATE_SCRIPT,
                instance.getSelectionStrategy());
    }

}
