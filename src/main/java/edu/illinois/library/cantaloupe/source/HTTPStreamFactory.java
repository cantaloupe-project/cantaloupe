package edu.illinois.library.cantaloupe.source;

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

    private static final HttpMethod HTTP_METHOD = HttpMethod.GET;
    private static final int WINDOW_SIZE        = (int) Math.pow(2, 19);

    private final HttpClient client;
    private final HttpSource.RequestInfo requestInfo;
    private final long contentLength;
    private final boolean useRanges;

    HTTPStreamFactory(HttpClient client,
                      HttpSource.RequestInfo requestInfo,
                      long contentLength,
                      boolean useRanges) {
        this.client        = client;
        this.requestInfo   = requestInfo;
        this.contentLength = contentLength;
        this.useRanges     = useRanges;
    }

    @Override
    public ImageInputStream newImageInputStream() throws IOException {
        if (useRanges) {
            final JettyHTTPImageInputStreamClient rangingClient =
                    new JettyHTTPImageInputStreamClient(client, requestInfo.getURI());
            rangingClient.setRequestTimeout(HttpSource.getRequestTimeout());
            rangingClient.setExtraRequestHeaders(requestInfo.getHeaders());
            return new HTTPImageInputStream(rangingClient, WINDOW_SIZE, contentLength);
        } else {
            return StreamFactory.super.newImageInputStream();
        }
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
                    .method(HTTP_METHOD);
            extraHeaders.forEach(h -> request.header(h.getName(), h.getValue()));

            LOGGER.trace("Requesting {} {} (extra headers: {})",
                    HTTP_METHOD, requestInfo.getURI(), extraHeaders);

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

}
