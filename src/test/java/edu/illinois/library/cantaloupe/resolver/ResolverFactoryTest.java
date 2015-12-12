package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.ConfigurationException;
import org.apache.commons.configuration.BaseConfiguration;

public class ResolverFactoryTest extends CantaloupeTestCase {

    public void testGetResolver() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        // FilesystemResolver
        config.setProperty("resolver", "FilesystemResolver");
        assertTrue(ResolverFactory.getResolver() instanceof FilesystemResolver);

        // HttpResolver
        config.setProperty("resolver", "HttpResolver");
        assertTrue(ResolverFactory.getResolver() instanceof HttpResolver);

        // invalid resolver
        try {
            config.setProperty("resolver", "bogus");
            ResolverFactory.getResolver();
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }

        // no resolver
        try {
            config.setProperty("resolver", null);
            ResolverFactory.getResolver();
            fail("Expected exception");
        } catch (ConfigurationException e) {
            // pass
        }
    }

}
