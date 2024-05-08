package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest extends BaseTest {

    private Client client;
    private Server server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new Server();
        server.setRoot(TestUtil.getFixturePath().resolve("images"));

        client = new Client();
        client.setTransport(Transport.HTTP1_1);
        client.setURI(server.getHTTPURI().resolve("/jpg"));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        try {
            client.stop();
        } finally {
            server.stop();
        }
    }

    @Test
    void testAcceptingRangesWhenSetToTrue() throws Exception {
        server.setAcceptingRanges(true);
        server.start();

        Response response = client.send();
        assertEquals("bytes", response.getHeaders().getFirstValue("Accept-Ranges"));
    }

    @Test
    void testAcceptingRangesWhenSetToFalse() throws Exception {
        server.setAcceptingRanges(false);
        server.start();

        Response response = client.send();
        assertEquals(0, response.getHeaders().getAll("Accept-Ranges").size());
    }

    @Test
    void testBasicAuthWithValidCredentials() throws Exception {
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
    void testBasicAuthWithInvalidCredentials() throws Exception {
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
    void testHandler() throws Exception {
        final String path = "/unauthorized";

        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
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
    void testHTTP1() throws Exception {
        server.setHTTP2Enabled(false);
        server.start();

        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertEquals(Transport.HTTP1_1, response.getTransport());
    }

    @Disabled // TODO: this used to pass using the Jetty HTTP client, but it fails using the JDK 10 client
    @Test
    void testHTTP2() throws Exception {
        server.setHTTP1Enabled(false);
        server.start();

        client.setTransport(Transport.HTTP2_0);

        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertEquals(Transport.HTTP2_0, response.getTransport());
    }

    @Test
    void testHTTPS1() throws Exception {
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
    void testHTTPS2() throws Exception {
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
