package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ResponseTest {

    private HttpClient jettyClient;
    private WebServer server;

    @Before
    public void setUp() throws Exception {
        server = new WebServer();
        server.start();

        HttpClientTransport transport = new HttpClientTransportOverHTTP();
        jettyClient = new HttpClient(transport, new SslContextFactory());
        jettyClient.start();
        jettyClient.setFollowRedirects(false);
    }

    @After
    public void tearDown() throws Exception {
        try {
            server.stop();
        } finally {
            jettyClient.stop();
        }
    }

    @Test
    public void testFromJettyResponse() throws Exception {
        Request request = jettyClient.newRequest(
                server.getHTTPURI().resolve("/jpg"));
        request.method(HttpMethod.GET);

        ContentResponse jresponse = request.send();

        Response response = Response.fromJettyResponse(jresponse);

        assertEquals(jresponse.getContent(), response.getBody());
        assertEquals(jresponse.getStatus(), response.getStatus());
        assertEquals(Transport.HTTP1_1, response.getTransport());

        Map<String,String> expectedHeaders = new HashMap<>();
        for (HttpField field : jresponse.getHeaders()) {
            expectedHeaders.put(field.getName(), field.getValue());
        }

        assertEquals(expectedHeaders, response.getHeaders());
    }

    @Test
    public void testGetBodyAsString() throws Exception {
        final byte[] body = "cats".getBytes("UTF-8");

        Response response = new Response();
        response.setBody(body);
        assertEquals(new String(body, "UTF-8"), response.getBodyAsString());
    }

}
