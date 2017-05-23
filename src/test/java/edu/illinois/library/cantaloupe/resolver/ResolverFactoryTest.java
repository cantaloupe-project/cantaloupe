package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResolverFactoryTest extends BaseTest {

    @Test
    public void testGetAllResolvers() {
        assertEquals(5, ResolverFactory.getAllResolvers().size());
    }

    @Test
    public void testGetResolverWithStaticResolver() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();

        Identifier identifier = new Identifier("jdbc");

        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof FilesystemResolver);

        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof HttpResolver);

        // invalid resolver
        try {
            config.setProperty(Key.RESOLVER_STATIC, "BogusResolver");
            ResolverFactory.getResolver(identifier);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetResolverUsingDelegateScript() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(Key.RESOLVER_DELEGATE, true);

        Identifier identifier = new Identifier("http");
        assertTrue(ResolverFactory.getResolver(identifier)
                instanceof HttpResolver);

        identifier = new Identifier("anythingelse");
        assertTrue(ResolverFactory.getResolver(identifier)
                instanceof FilesystemResolver);
    }

    @Test
    public void testGetSelectionStrategy() {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();

        config.setProperty(Key.RESOLVER_DELEGATE, "false");
        assertEquals(ResolverFactory.SelectionStrategy.STATIC,
                ResolverFactory.getSelectionStrategy());

        config.setProperty(Key.RESOLVER_DELEGATE, "true");
        assertEquals(ResolverFactory.SelectionStrategy.DELEGATE_SCRIPT,
                ResolverFactory.getSelectionStrategy());
    }

}
