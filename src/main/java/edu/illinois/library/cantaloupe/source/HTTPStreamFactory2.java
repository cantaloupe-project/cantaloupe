package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

import static edu.illinois.library.cantaloupe.source.HttpSource2.LOGGER;
import static edu.illinois.library.cantaloupe.source.HttpSource2.getHTTPClient;

/**
 * Source of streams for {@link HttpSource2}, returned from {@link
 * HttpSource2#newStreamFactory()}.
 */
final class HTTPStreamFactory2 implements StreamFactory {

    private static final int DEFAULT_CHUNK_SIZE       = (int) Math.pow(2, 19);
    private static final int DEFAULT_CHUNK_CACHE_SIZE = (int) Math.pow(1024, 2);

    private final OkHttpClient client;
    private final HTTPRequestInfo requestInfo;
    private final long contentLength;
    private final boolean serverAcceptsRanges;

    HTTPStreamFactory2(OkHttpClient client,
                       HTTPRequestInfo requestInfo,
                       long contentLength,
                       boolean serverAcceptsRanges) {
        this.client              = client;
        this.requestInfo         = requestInfo;
        this.contentLength       = contentLength;
        this.serverAcceptsRanges = serverAcceptsRanges;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        final Headers extraHeaders = requestInfo.getHeaders();

        Request.Builder builder = new Request.Builder()
                .url(requestInfo.getURI());
        extraHeaders.forEach(h ->
                builder.addHeader(h.getName(), h.getValue()));

        if (requestInfo.getUsername() != null &&
                requestInfo.getSecret() != null) {
            builder.addHeader("Authorization",
                    "Basic " + requestInfo.getBasicAuthToken());
        }

        LOGGER.trace("Requesting GET {} (extra headers: {})",
                requestInfo.getURI(), extraHeaders);

        Request request   = builder.build();
        Response response = getHTTPClient().newCall(request).execute();
        ResponseBody body = response.body();

        return (body != null) ? body.byteStream() : null;
    }

    @Override
    public ImageInputStream newSeekableStream() throws IOException {
        if (isChunkingEnabled()) {
            if (serverAcceptsRanges) {
                final int chunkSize = getChunkSize();
                LOGGER.debug("newSeekableStream(): using {}-byte chunks",
                        chunkSize);
                OkHttpHTTPImageInputStreamClient rangingClient =
                        new OkHttpHTTPImageInputStreamClient(
                                client, requestInfo.getURI());
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

    @Override
    public boolean isSeekingDirect() {
        return isChunkingEnabled();
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
