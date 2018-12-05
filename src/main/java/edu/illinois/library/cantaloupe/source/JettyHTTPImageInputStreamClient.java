package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Range;

import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation backed by a Jetty HTTP client.
 */
class JettyHTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JettyHTTPImageInputStreamClient.class);

    private HttpClient jettyClient;
    private String uri;
    private Headers extraHeaders      = new Headers();
    private int requestTimeoutSeconds = 300;

    /**
     * Converts a response from Jetty into a {@link Response}.
     */
    private static Response toResponse(ContentResponse jettyResponse) {
        Response response = new Response();
        response.setStatus(jettyResponse.getStatus());
        jettyResponse.getHeaders().forEach(jh ->
                response.getHeaders().add(jh.getName(), jh.getValue()));
        return response;
    }

    /**
     * Converts a response from Jetty into a {@link Response}.
     */
    private static Response toResponse(org.eclipse.jetty.client.api.Response jettyResponse,
                                       InputStreamResponseListener listener) throws IOException {
        byte[] entity;
        try (InputStream is = listener.getInputStream()) {
            entity = IOUtils.toByteArray(is);
        }
        Response response = new Response();
        response.setStatus(jettyResponse.getStatus());
        response.setBody(entity);
        jettyResponse.getHeaders().forEach(jh ->
                response.getHeaders().add(jh.getName(), jh.getValue()));
        return response;
    }

    JettyHTTPImageInputStreamClient(HttpClient jettyClient, String uri) {
        this.jettyClient = jettyClient;
        this.uri = uri;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        Request request = jettyClient
                .newRequest(uri)
                .timeout(requestTimeoutSeconds, TimeUnit.SECONDS)
                .method(HttpMethod.HEAD);
        extraHeaders.forEach(h -> request.header(h.getName(), h.getValue()));

        LOGGER.trace("Requesting HEAD {} (extra headers: {})",
                uri, extraHeaders);

        try {
            ContentResponse jettyResponse = request.send();
            return toResponse(jettyResponse);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        try {
            InputStreamResponseListener listener =
                    new InputStreamResponseListener();

            extraHeaders.set("Range",
                    "bytes=" + range.start + "-" + range.end);

            Request request = jettyClient
                    .newRequest(uri)
                    .timeout(requestTimeoutSeconds, TimeUnit.SECONDS)
                    .method(HttpMethod.GET);
            extraHeaders.forEach(h -> request.header(h.getName(), h.getValue()));

            LOGGER.trace("Requesting GET {} (extra headers: {})",
                    uri, extraHeaders);

            request.send(listener);

            // Wait for the response headers to arrive.
            org.eclipse.jetty.client.api.Response jettyResponse = listener.get(
                    requestTimeoutSeconds, TimeUnit.SECONDS);

            if (jettyResponse.getStatus() == HttpStatus.OK_200 ||
                    jettyResponse.getStatus() == HttpStatus.PARTIAL_CONTENT_206) {
                return toResponse(jettyResponse, listener);
            } else {
                throw new IOException("Unexpected HTTP response code: " +
                        jettyResponse.getStatus());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    void setExtraRequestHeaders(Headers headers) {
        this.extraHeaders = new Headers(headers);
    }

    void setRequestTimeout(int seconds) {
        this.requestTimeoutSeconds = seconds;
    }

}
