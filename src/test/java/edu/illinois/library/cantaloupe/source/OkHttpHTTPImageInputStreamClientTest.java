package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

public class OkHttpHTTPImageInputStreamClientTest extends BaseTest {

    private WebServer server;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new WebServer();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    void sendHEADRequest() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("HEAD", baseRequest.getMethod());
                response.setStatus(200);
                baseRequest.setHandled(true);
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo =
                new HTTPRequestInfo(server.getHTTPURI().toString());
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendHEADRequest();
    }

    @Test
    void sendHEADRequestSendsRequestInfoCredentials() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("Basic dXNlcjpzZWNyZXQ=",
                        baseRequest.getHeader("Authorization"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo(
                server.getHTTPURI().toString(), "user", "secret");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendHEADRequest();
    }

    @Test
    void sendHEADRequestSendsRequestInfoHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", baseRequest.getHeader("X-Cats"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo =
                new HTTPRequestInfo(server.getHTTPURI().toString());
        requestInfo.getHeaders().add("X-Cats", "yes");

        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendHEADRequest();
    }

    @Test
    void sendGETRequest() throws Exception {
        server.start();

        final HTTPRequestInfo requestInfo =
                new HTTPRequestInfo(server.getHTTPURI() + "/jpg");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        Response response = instance.sendGETRequest(new Range(0, 1, 4));

        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xd8 },
                response.getBody());
        assertEquals(206, response.getStatus());
    }

    @Test
    void sendGETRequestSendsRequestInfoCredentials() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("Basic dXNlcjpzZWNyZXQ=",
                        baseRequest.getHeader("Authorization"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo = new HTTPRequestInfo(
                server.getHTTPURI() + "/jpg", "user", "secret");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendGETRequest(new Range(0, 1, 4));
    }

    @Test
    void sendGETRequestSendsRequestInfoHeaders() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", baseRequest.getHeader("X-Cats"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        final HTTPRequestInfo requestInfo =
                new HTTPRequestInfo(server.getHTTPURI() + "/jpg");
        requestInfo.getHeaders().add("X-Cats", "yes");
        final OkHttpHTTPImageInputStreamClient instance =
                new OkHttpHTTPImageInputStreamClient(requestInfo);

        instance.sendGETRequest(new Range(0, 1, 4));
    }

}
