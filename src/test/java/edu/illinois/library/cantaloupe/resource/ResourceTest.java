package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebServer;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.io.IOException;

public abstract class ResourceTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";
    protected static final Integer PORT = TestUtil.getOpenPort();

    protected static Client client = new Client(new Context(), Protocol.HTTP);

    public static void resetConfiguration() throws IOException {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty("print_stack_trace_on_error_pages", false);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty("processor.fallback", "Java2dProcessor");
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty("FilesystemResolver.lookup_strategy",
                "BasicLookupStrategy");
        config.setProperty("FilesystemResolver.BasicLookupStrategy.path_prefix",
                TestUtil.getFixturePath() + "/images/");
    }

    @Before
    public void setUp() throws Exception {
        resetConfiguration();
        WebServer webServer = StandaloneEntry.getWebServer();
        webServer.setHttpEnabled(true);
        webServer.setHttpPort(PORT);
        webServer.start();
    }

    @After
    public void tearDown() throws Exception {
        StandaloneEntry.getWebServer().stop();
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
