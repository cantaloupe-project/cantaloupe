package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;

import java.io.IOException;

/**
 * Adapts any HTTP client to work with {@link HTTPImageInputStream}. The client
 * should be initialized to the URI of the resource that the stream is supposed
 * to access.
 */
public interface HTTPImageInputStreamClient {

    /**
     * @return Response. In particular, the {@link Response#getStatus() status}
     *         is set, and {@literal Accept-Ranges} and {@literal
     *         Content-Length} {@link Response#getHeaders() headers} are
     *         included if the server sent them.
     * @throws IOException upon failure to receive a valid response
     *         (<strong>not</strong> a response with an error status code).
     */
    Response sendHEADRequest() throws IOException;

    /**
     * @param range Byte range to request.
     * @return      Same as {@link #sendHEADRequest()}, but the {@link
     *              Response#getBody() body} is also included.
     * @throws IOException upon failure to receive a valid response
     *         (<strong>not</strong> a response with an error status code).
     */
    Response sendGETRequest(Range range) throws IOException;

}
