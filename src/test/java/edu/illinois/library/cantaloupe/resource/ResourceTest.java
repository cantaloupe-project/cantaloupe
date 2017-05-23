package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebServer;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

public abstract class ResourceTest extends BaseTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";
    protected static final Integer PORT = TestUtil.getOpenPort();

    protected static WebServer webServer;

    protected Client client;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                TestUtil.getFixturePath() + "/images/");

        client = new Client(new Context(), Protocol.HTTP);
        client.start();

        webServer = StandaloneEntry.getWebServer();
        webServer.setHttpEnabled(true);
        webServer.setHttpPort(PORT);
        webServer.start();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        client.stop();
        webServer.stop();
    }

    protected ClientResource getClientForUriPath(String path) {
        Reference url = new Reference("http://localhost:" + PORT + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

    protected ClientResource getClientForUriPath(String path,
                                                 String username,
                                                 String secret) {
        ClientResource resource = getClientForUriPath(path);
        resource.setChallengeResponse(new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC, username, secret));
        return resource;
    }

}
