package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.ApplicationServer;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.junit.After;
import org.junit.Before;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;

/**
 * Abstract base class for functional HTTP endpoint tests.
 */
public abstract class ResourceTest extends BaseTest {

    protected static int HTTP_PORT = SocketUtils.getOpenPort();
    protected static int HTTPS_PORT;

    protected static ApplicationServer appServer;

    protected Client client;

    static {
        do {
            HTTPS_PORT = SocketUtils.getOpenPort();
        } while (HTTPS_PORT == HTTP_PORT);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
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

        new CacheFacade().purge();

        // Enable HTTP and HTTPS over HTTP/1.1 and HTTP/2
        appServer = StandaloneEntry.getAppServer();
        appServer.setHTTPEnabled(true);
        appServer.setHTTPPort(HTTP_PORT);
        appServer.setInsecureHTTP2Enabled(true);

        appServer.setHTTPSEnabled(true);
        appServer.setHTTPSPort(HTTPS_PORT);
        appServer.setHTTPSKeyStoreType("JKS");
        appServer.setHTTPSKeyStorePath(
                TestUtil.getFixture("keystore.jks").getAbsolutePath());
        appServer.setHTTPSKeyStorePassword("password");
        appServer.setHTTPSKeyPassword("password");
        appServer.setSecureHTTP2Enabled(SystemUtils.isALPNAvailable());

        appServer.start();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        appServer.stop();

        if (client != null) {
            client.stop();
        }
    }

    abstract protected String getEndpointPath();

    /**
     * @param path URI path relative to {@link #getEndpointPath()}.
     * @return HTTP URI. Subclasses should override if they need an HTTPS URI.
     */
    protected URI getHTTPURI(String path) {
        try {
            return new URI("http://localhost:" + appServer.getHTTPPort() +
                    getEndpointPath() + path);
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * @param path URI path relative to {@link #getEndpointPath()}.
     * @return HTTPS URI.
     */
    protected URI getHTTPSURI(String path) {
        try {
            URI uri = getHTTPURI(path);
            return new URI("https", uri.getUserInfo(), uri.getHost(),
                    appServer.getHTTPSPort(), uri.getPath(), uri.getQuery(),
                    uri.getFragment());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * @param subpath URI path to use, relative to {@link #getEndpointPath()}.
     * @return New client instance. Clients should call {@link Client#stop()}
     *         on it when they are done with it. Or, if they assign it to
     *         {@link #client}, {@link #tearDown()} will take care of it.
     */
    protected Client newClient(String subpath) {
        return new Client().builder().uri(getHTTPURI(subpath)).build();
    }

    /**
     * @param subpath URI path to use, relative to {@link #getEndpointPath()}.
     * @param user
     * @param secret
     * @param realm
     * @return New client instance, initialized to use HTTP Basic
     *         authentication. Clients should call {@link Client#stop()}
     *         on it when they are done. Or, if they assign it to
     *         {@link #client}, {@link #tearDown()} will take care of it.
     */
    protected Client newClient(String subpath, String user, String secret,
                               String realm) {
        return new Client().builder().
                uri(getHTTPURI(subpath)).
                realm(realm).
                username(user).
                secret(secret).
                build();
    }

}
