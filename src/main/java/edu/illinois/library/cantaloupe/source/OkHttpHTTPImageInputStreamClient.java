package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Range;

import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Map;

/**
 * Implementation backed by an {@link OkHttpClient}.
 */
class OkHttpHTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private final HTTPRequestInfo requestInfo;

    /**
     * @return New instance corresponding to the argument.
     */
    private static Response toResponse(okhttp3.Response okHttpResponse)
            throws IOException {
        Response response = new Response();
        // Set status code
        response.setStatus(okHttpResponse.code());
        // Set headers
        okHttpResponse.headers().toMultimap().forEach((k, list) ->
                list.forEach(v -> response.getHeaders().add(k, v)));
        // Set body
        ResponseBody body = okHttpResponse.body();
        if (body != null) {
            response.setBody(body.bytes());
        }
        return response;
    }

    OkHttpHTTPImageInputStreamClient(HTTPRequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        try (okhttp3.Response response =
                     HttpSource.request(requestInfo, "HEAD")) {
            return toResponse(response);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        final Map<String,String> extraHeaders =
                Map.of("Range", "bytes=" + range.start + "-" + range.end);
        try (okhttp3.Response okHttpResponse =
                     HttpSource.request(requestInfo, "GET", extraHeaders)) {
            if (okHttpResponse.code() == 200 || okHttpResponse.code() == 206) {
                return toResponse(okHttpResponse);
            } else {
                throw new IOException("Unexpected HTTP response code: " +
                        okHttpResponse.code());
            }
        }
    }

}
