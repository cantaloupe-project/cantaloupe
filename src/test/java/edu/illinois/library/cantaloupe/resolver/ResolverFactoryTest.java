package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResolverFactoryTest {

    @Test
    public void testGetAllResolvers() {
        assertEquals(5, ResolverFactory.getAllResolvers().size());
    }

    @Test
    public void testGetResolverWithStaticResolver() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);
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

        // no resolver
        try {
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY, null);
            ResolverFactory.getResolver(identifier);
            fail("Expected exception");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    @Test
    public void testGetResolverUsingDelegateScript() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, true);
        Application.setConfiguration(config);

        Identifier identifier = new Identifier("http");
        assertTrue(ResolverFactory.getResolver(identifier)
                instanceof HttpResolver);

        identifier = new Identifier("anythingelse");
        assertTrue(ResolverFactory.getResolver(identifier)
                instanceof FilesystemResolver);
    }

    @Test
    public void testGetSelectionStrategy() {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        config.setProperty(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, "false");
        assertEquals(ResolverFactory.SelectionStrategy.STATIC,
                ResolverFactory.getSelectionStrategy());

        config.setProperty(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, "true");
        assertEquals(ResolverFactory.SelectionStrategy.DELEGATE_SCRIPT,
                ResolverFactory.getSelectionStrategy());
    }

}
