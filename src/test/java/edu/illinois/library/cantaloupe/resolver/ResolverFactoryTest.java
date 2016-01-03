package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResolverFactoryTest {

    @Test
    public void testGetResolverWithChooserScript() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("resolver.chooser_script",
                TestUtil.getFixture("get_resolver.rb").getAbsolutePath());
        Application.setConfiguration(config);

        // identifier-resolver match
        Identifier identifier = new Identifier("http");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof HttpResolver);
    }

    @Test
    public void testGetResolverWithStaticResolver() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);
        Identifier identifier = new Identifier("jdbc");

        config.setProperty(ResolverFactory.RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof FilesystemResolver);

        config.setProperty(ResolverFactory.RESOLVER_CONFIG_KEY,
                "HttpResolver");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof HttpResolver);

        // invalid resolver
        try {
            config.setProperty(ResolverFactory.RESOLVER_CONFIG_KEY,
                    "bogus");
            ResolverFactory.getResolver(identifier);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }

        // no resolver
        try {
            config.setProperty(ResolverFactory.RESOLVER_CONFIG_KEY, null);
            ResolverFactory.getResolver(identifier);
            fail("Expected exception");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    @Test
    public void testGetResolverPrefersChooserScript() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("resolver.", "JdbcResolver");
        config.setProperty("resolver.chooser_script",
                TestUtil.getFixture("get_resolver.rb").getAbsolutePath());
        Application.setConfiguration(config);

        Identifier identifier = new Identifier("http");
        assertTrue(ResolverFactory.getResolver(identifier) instanceof HttpResolver);
    }

}
