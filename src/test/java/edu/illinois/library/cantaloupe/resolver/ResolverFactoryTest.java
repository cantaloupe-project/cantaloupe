package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

public class ResolverFactoryTest extends TestCase {

    public void testGetResolver() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        config.setProperty("resolver", "FilesystemResolver");
        assertTrue(ResolverFactory.getResolver() instanceof FilesystemResolver);

        config.setProperty("resolver", "HttpResolver");
        assertTrue(ResolverFactory.getResolver() instanceof HttpResolver);

        try {
            config.setProperty("resolver", "bogus");
            assertNull(ResolverFactory.getResolver());
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

}
