package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest extends BaseTest {

    private HttpClient jettyClient;
    private WebServer server;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new WebServer();
        server.start();

        HttpClientTransport transport = new HttpClientTransportOverHTTP();
        jettyClient = new HttpClient(transport, new SslContextFactory());
        jettyClient.start();
        jettyClient.setFollowRedirects(false);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        try {
            server.stop();
        } finally {
            jettyClient.stop();
        }
    }

    @Test
    void testFromJettyResponse() throws Exception {
        Request request = jettyClient.newRequest(
                server.getHTTPURI().resolve("/jpg"));
        request.method(HttpMethod.GET);

        ContentResponse jresponse = request.send();

        Response response = Response.fromJettyResponse(jresponse);

        assertEquals(jresponse.getContent(), response.getBody());
        assertEquals(jresponse.getStatus(), response.getStatus());
        assertEquals(Transport.HTTP1_1, response.getTransport());

        Headers expectedHeaders = new Headers();
        for (HttpField field : jresponse.getHeaders()) {
            expectedHeaders.add(field.getName(), field.getValue());
        }

        assertEquals(expectedHeaders, response.getHeaders());
    }

    @Test
    void testGetBodyAsString() {
        final byte[] body = "cats".getBytes(StandardCharsets.UTF_8);

        Response response = new Response();
        response.setBody(body);
        assertEquals(new String(body, StandardCharsets.UTF_8),
                response.getBodyAsString());
    }

}
