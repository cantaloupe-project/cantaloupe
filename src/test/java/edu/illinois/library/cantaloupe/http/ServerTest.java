package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class ServerTest extends BaseTest {

    private Client client;
    private Server server;

    @Override
    @Before
    public void setUp() throws Exception {
        server = new Server();
        server.setRoot(TestUtil.getFixturePath().resolve("images"));

        client = new Client();
        client.setTransport(Transport.HTTP1_1);
        client.setURI(server.getHTTPURI().resolve("/jpg"));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            client.stop();
        } finally {
            server.stop();
        }
    }

    @Test
    public void testAcceptingRangesWhenSetToTrue() throws Exception {
        server.setAcceptingRanges(true);
        server.start();

        Response response = client.send();
        assertEquals("bytes", response.getHeaders().getFirstValue("Accept-Ranges"));
    }

    @Test
    public void testAcceptingRangesWhenSetToFalse() throws Exception {
        server.setAcceptingRanges(false);
        server.start();

        Response response = client.send();
        assertEquals(0, response.getHeaders().getAll("Accept-Ranges").size());
    }

    @Test
    public void testBasicAuthWithValidCredentials() throws Exception {
        final String realm = "Test Realm";
        final String user = "dogs";
        final String secret = "monkeys";

        server.setBasicAuthEnabled(true);
        server.setAuthRealm(realm);
        server.setAuthUser(user);
        server.setAuthSecret(secret);
        server.start();

        client.setRealm(realm);
        client.setUsername(user);
        client.setSecret(secret);

        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBasicAuthWithInvalidCredentials() throws Exception {
        final String realm = "Test Realm";
        final String user = "dogs";

        server.setBasicAuthEnabled(true);
        server.setAuthRealm(realm);
        server.setAuthUser(user);
        server.setAuthSecret("bugs");
        server.start();

        client.setRealm(realm);
        client.setUsername(user);
        client.setSecret("horses");

        try {
            client.send();
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testHandler() throws Exception {
        final String path = "/unauthorized";

        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {
                if (baseRequest.getPathInfo().startsWith(path)) {
                    response.setStatus(500);
                }
                baseRequest.setHandled(true);
            }
        });
        server.start();

        client.setURI(server.getHTTPURI().resolve(path));
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(500, e.getStatusCode());
        }
    }

    @Test
    public void testHTTP1() throws Exception {
        server.setHTTP2Enabled(false);
        server.start();

        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertEquals(Transport.HTTP1_1, response.getTransport());
    }

    @Test
    public void testHTTP2() throws Exception {
        server.setHTTP1Enabled(false);
        server.start();

        client.setTransport(Transport.HTTP2_0);

        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertEquals(Transport.HTTP2_0, response.getTransport());
    }

    @Test
    public void testHTTPS1() throws Exception {
        server.setHTTPS1Enabled(true);
        server.setHTTPS2Enabled(false);
        server.setKeyManagerPassword("password");
        server.setKeyStorePassword("password");
        server.setKeyStorePath(TestUtil.getFixture("keystore-password.jks"));
        server.start();

        client.setTransport(Transport.HTTP1_1);

        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertEquals(Transport.HTTP1_1, response.getTransport());
    }

    @Test
    public void testHTTPS2() throws Exception {
        assumeTrue(SystemUtils.isALPNAvailable());

        server.setHTTPS1Enabled(false);
        server.setHTTPS2Enabled(true);
        server.setKeyManagerPassword("password");
        server.setKeyStorePassword("password");
        server.setKeyStorePath(TestUtil.getFixture("keystore-password.jks"));
        server.start();

        client.setTransport(Transport.HTTP2_0);

        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertEquals(Transport.HTTP2_0, response.getTransport());
    }

}
