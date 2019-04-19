package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

public class JettyHTTPImageInputStreamClientTest extends BaseTest {

    private WebServer server;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new WebServer();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    void testSendHEADRequest() throws Exception {
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

        final String uri = server.getHTTPURI().toString();
        final HttpClient jettyClient = new HttpClient();
        jettyClient.start();

        final JettyHTTPImageInputStreamClient instance =
                new JettyHTTPImageInputStreamClient(jettyClient, uri);

        instance.sendHEADRequest();
    }

    @Test
    void testSendHEADRequestSendsExtraHeaders() throws Exception {
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

        final String uri = server.getHTTPURI().toString();
        final HttpClient jettyClient = new HttpClient();
        jettyClient.start();

        final JettyHTTPImageInputStreamClient instance =
                new JettyHTTPImageInputStreamClient(jettyClient, uri);
        Headers extraHeaders = new Headers();
        extraHeaders.add("X-Cats", "yes");
        instance.setExtraRequestHeaders(extraHeaders);

        instance.sendHEADRequest();
    }

    @Test
    void testSendGETRequest() throws Exception {
        server.start();

        final String uri = server.getHTTPURI().toString() + "/jpg";
        final HttpClient jettyClient = new HttpClient();
        jettyClient.start();

        final JettyHTTPImageInputStreamClient instance =
                new JettyHTTPImageInputStreamClient(jettyClient, uri);

        Response response = instance.sendGETRequest(new Range(0, 1, 4));

        assertArrayEquals(new byte[] { (byte) 0xff, (byte) 0xd8 }, response.getBody());
        assertEquals(206, response.getStatus());
    }

    @Test
    void testSendGETRequestSendsExtraHeaders() throws Exception {
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

        final String uri = server.getHTTPURI().toString() + "/jpg";
        final HttpClient jettyClient = new HttpClient();
        jettyClient.start();

        final JettyHTTPImageInputStreamClient instance =
                new JettyHTTPImageInputStreamClient(jettyClient, uri);
        Headers extraHeaders = new Headers();
        extraHeaders.add("X-Cats", "yes");
        instance.setExtraRequestHeaders(extraHeaders);

        instance.sendGETRequest(new Range(0, 1, 4));
    }

}
