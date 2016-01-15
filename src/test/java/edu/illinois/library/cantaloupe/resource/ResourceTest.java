package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.After;
import org.junit.Before;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.io.File;
import java.io.IOException;

public abstract class ResourceTest {

    protected static final Integer PORT = TestUtil.getOpenPort();

    protected static Client client = new Client(new Context(), Protocol.HTTP);

    public static BaseConfiguration newConfiguration() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("print_stack_trace_on_error_pages", false);
        config.setProperty("delegate_script",
                TestUtil.getFixture("delegate.rb").getAbsolutePath());
        config.setProperty("http.enabled", true);
        config.setProperty("http.port", PORT);
        config.setProperty("processor.fallback", "Java2dProcessor");
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty("FilesystemResolver.lookup_strategy",
                "BasicLookupStrategy");
        config.setProperty("FilesystemResolver.BasicLookupStrategy.path_prefix",
                TestUtil.getFixturePath() + File.separator);
        return config;
    }

    @Before
    public void setUp() throws Exception {
        Application.setConfiguration(newConfiguration());
        Application.startServer();
    }

    @After
    public void tearDown() throws Exception {
        Application.stopServer();
    }

    protected ClientResource getClientForUriPath(String path) {
        Reference url = new Reference(getBaseUri() + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

    protected String getBaseUri() {
        return "http://localhost:" + PORT;
    }

}
