package edu.illinois.library.cantaloupe.source;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

final class HTTPStreamFactory implements StreamFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HTTPStreamFactory.class);

    private final HttpClient client;
    private final URI uri;

    HTTPStreamFactory(HttpClient client, URI uri) {
        this.client = client;
        this.uri = uri;
    }

    @Override
    public InputStream newInputStream() {
        try {
            InputStreamResponseListener listener =
                    new InputStreamResponseListener();
            client.newRequest(uri).
                    timeout(HttpSource.getRequestTimeout(), TimeUnit.SECONDS).
                    method(HttpMethod.GET).
                    send(listener);

            // Wait for the response headers to arrive.
            Response response = listener.get(HttpSource.getRequestTimeout(),
                    TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                return listener.getInputStream();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
