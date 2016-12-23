package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Identifier;
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

        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof FilesystemResolver);

        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "HttpResolver");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof HttpResolver);

        // invalid resolver
        try {
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                    "bogus");
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
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, true);

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

        config.setProperty(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, "false");
        assertEquals(ResolverFactory.SelectionStrategy.STATIC,
                ResolverFactory.getSelectionStrategy());

        config.setProperty(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, "true");
        assertEquals(ResolverFactory.SelectionStrategy.DELEGATE_SCRIPT,
                ResolverFactory.getSelectionStrategy());
    }

}
