package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest extends BaseTest {

    private HttpClient javaClient;
    private WebServer server;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        server = new WebServer();
        server.start();

        javaClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    @Test
    void testFromHttpClientResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(server.getHTTPURI().resolve("/jpg"))
                .build();

        HttpResponse<byte[]> jresponse = javaClient.send(
                request, HttpResponse.BodyHandlers.ofByteArray());

        Response response = Response.fromHttpClientResponse(jresponse);

        assertEquals(jresponse.body(), response.getBody());
        assertEquals(jresponse.statusCode(), response.getStatus());
        assertEquals(Transport.HTTP2_0, response.getTransport());

        Headers expectedHeaders = new Headers();
        jresponse.headers().map().forEach((name, list) ->
                list.forEach(h ->
                        expectedHeaders.add(name, h)));

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
