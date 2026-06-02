package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    /**
     * Asynchronous variant of {@link #sendGETRequest(Range)}. The default
     * implementation wraps the synchronous call on the common ForkJoin pool;
     * implementations backed by a non-blocking transport should override this
     * to return a future tied to the transport's event loop, so that multiple
     * range requests can overlap on a single thread.
     */
    default CompletableFuture<Response> sendGETRequestAsync(Range range) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendGETRequest(range);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

}
