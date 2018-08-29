package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Header;
import edu.illinois.library.cantaloupe.http.Headers;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Returned from {@link HttpSource#newStreamFactory()}.
 */
final class HTTPStreamFactory implements StreamFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HTTPStreamFactory.class);

    private static final HttpMethod HTTP_METHOD = HttpMethod.GET;

    private final HttpClient client;
    private final HttpSource.RequestInfo requestInfo;

    HTTPStreamFactory(HttpClient client, HttpSource.RequestInfo requestInfo) {
        this.client = client;
        this.requestInfo = requestInfo;
    }

    @Override
    public InputStream newInputStream() {
        try {
            InputStreamResponseListener listener =
                    new InputStreamResponseListener();

            final Headers headers = new Headers();
            for (String name : requestInfo.getHeaders().keySet()) {
                headers.add(name, requestInfo.getHeaders().get(name).toString());
            }

            Request request = client.newRequest(requestInfo.getURI()).
                    timeout(HttpSource.getRequestTimeout(), TimeUnit.SECONDS).
                    method(HTTP_METHOD);
            for (Header header : headers) {
                request.header(header.getName(), header.getValue());
            }

            LOGGER.debug("Requesting {} {} (extra headers: {})",
                    HTTP_METHOD, requestInfo.getURI(), headers);

            request.send(listener);

            // Wait for the response headers to arrive.
            Response response = listener.get(
                    HttpSource.getRequestTimeout(), TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                return listener.getInputStream();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
