package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static edu.illinois.library.cantaloupe.source.HttpSource.LOGGER;

/**
 * Source of streams for {@link HttpSource}, returned from {@link
 * HttpSource#newStreamFactory()}.
 */
final class HTTPStreamFactory implements StreamFactory {

    private static final int DEFAULT_CHUNK_SIZE       = (int) Math.pow(2, 19);
    private static final int DEFAULT_CHUNK_CACHE_SIZE = (int) Math.pow(1024, 2);

    private final HttpClient client;
    private final HttpSource.RequestInfo requestInfo;
    private final long contentLength;
    private final boolean serverAcceptsRanges;

    HTTPStreamFactory(HttpClient client,
                      HttpSource.RequestInfo requestInfo,
                      long contentLength,
                      boolean serverAcceptsRanges) {
        this.client              = client;
        this.requestInfo         = requestInfo;
        this.contentLength       = contentLength;
        this.serverAcceptsRanges = serverAcceptsRanges;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        try {
            InputStreamResponseListener listener =
                    new InputStreamResponseListener();

            final Headers extraHeaders = requestInfo.getHeaders();

            Request request = client
                    .newRequest(requestInfo.getURI())
                    .timeout(HttpSource.getRequestTimeout(), TimeUnit.SECONDS)
                    .method(HttpMethod.GET);
            extraHeaders.forEach(h -> request.header(h.getName(), h.getValue()));

            LOGGER.trace("Requesting GET {} (extra headers: {})",
                    requestInfo.getURI(), extraHeaders);

            request.send(listener);

            // Wait for the response headers to arrive.
            Response response = listener.get(
                    HttpSource.getRequestTimeout(), TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                return listener.getInputStream();
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new IOException(e);
        }
        return null;
    }

    @Override
    public ImageInputStream newSeekableStream() throws IOException {
        if (isChunkingEnabled()) {
            if (serverAcceptsRanges) {
                final int chunkSize = getChunkSize();
                LOGGER.debug("newSeekableStream(): using {}-byte chunks",
                        chunkSize);
                final JettyHTTPImageInputStreamClient rangingClient =
                        new JettyHTTPImageInputStreamClient(client, requestInfo.getURI());
                rangingClient.setRequestTimeout(HttpSource.getRequestTimeout());
                rangingClient.setExtraRequestHeaders(requestInfo.getHeaders());

                HTTPImageInputStream stream = new HTTPImageInputStream(
                        rangingClient, contentLength);
                stream.setWindowSize(chunkSize);
                if (isChunkCacheEnabled()) {
                    stream.setMaxChunkCacheSize(getMaxChunkCacheSize());
                }
                return stream;
            } else {
                LOGGER.debug("newSeekableStream(): chunking is enabled, but " +
                        "won't be used because the server's HEAD response " +
                        "didn't include an Accept-Ranges header.");
            }
        } else {
            LOGGER.debug("newSeekableStream(): chunking is disabled");
        }
        return StreamFactory.super.newSeekableStream();
    }

    private boolean isChunkingEnabled() {
        return Configuration.getInstance().getBoolean(
                Key.HTTPSOURCE_CHUNKING_ENABLED, true);
    }

    private int getChunkSize() {
        return (int) Configuration.getInstance().getLongBytes(
                Key.HTTPSOURCE_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    }

    private boolean isChunkCacheEnabled() {
        return Configuration.getInstance().getBoolean(
                Key.HTTPSOURCE_CHUNK_CACHE_ENABLED, true);
    }

    private int getMaxChunkCacheSize() {
        return (int) Configuration.getInstance().getLongBytes(
                Key.HTTPSOURCE_CHUNK_CACHE_MAX_SIZE, DEFAULT_CHUNK_CACHE_SIZE);
    }

}
