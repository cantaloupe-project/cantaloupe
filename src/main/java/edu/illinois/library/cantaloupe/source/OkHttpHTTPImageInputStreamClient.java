package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Range;

import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Implementation backed by an {@link OkHttpClient}.
 */
class OkHttpHTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OkHttpHTTPImageInputStreamClient.class);

    private OkHttpClient okHttpClient;
    private String uri;
    private Headers extraHeaders = new Headers();

    /**
     * Converts a {@link okhttp3.Response} into a {@link Response}.
     */
    private static Response toResponse(okhttp3.Response okHttpResponse)
            throws IOException {
        Response response = new Response();
        response.setStatus(okHttpResponse.code());

        ResponseBody body = okHttpResponse.body();
        if (body != null) {
            response.setBody(body.bytes());
        }

        okHttpResponse.headers().toMultimap().forEach((k, list) ->
                list.forEach(v -> response.getHeaders().add(k, v)));
        return response;
    }

    OkHttpHTTPImageInputStreamClient(OkHttpClient okHttpClient, String uri) {
        this.okHttpClient = okHttpClient;
        this.uri = uri;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        Request.Builder builder = new Request.Builder()
                .method("HEAD", null)
                .url(uri)
                .addHeader("User-Agent", HttpSource.getUserAgent());
        extraHeaders.forEach(h -> builder.addHeader(h.getName(), h.getValue()));

        Request request = builder.build();

        LOGGER.trace("Requesting HEAD {} (extra headers: {})",
                uri, extraHeaders);

        try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            return toResponse(response);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(uri)
                .addHeader("Range", "bytes=" + range.start + "-" + range.end)
                .addHeader("User-Agent", HttpSource.getUserAgent());
        extraHeaders.forEach(h -> builder.addHeader(h.getName(), h.getValue()));

        Request request = builder.build();

        LOGGER.trace("Requesting GET {} (extra headers: {})",
                uri, extraHeaders);

        try (okhttp3.Response okHttpResponse =
                     okHttpClient.newCall(request).execute()) {
            if (okHttpResponse.code() == 200 || okHttpResponse.code() == 206) {
                return toResponse(okHttpResponse);
            } else {
                throw new IOException("Unexpected HTTP response code: " +
                        okHttpResponse.code());
            }
        }
    }

    void setExtraRequestHeaders(Headers headers) {
        this.extraHeaders = new Headers(headers);
    }

}
