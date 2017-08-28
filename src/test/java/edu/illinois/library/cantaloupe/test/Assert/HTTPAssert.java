package edu.illinois.library.cantaloupe.test.Assert;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public final class HTTPAssert {

    public static void assertConnectionRefused(String uri) {
        try {
            HttpClient httpClient = newClient();
            Request request = httpClient.newRequest(uri);
            request.method("GET");
            request.send();
            fail("Connection not refused: " + uri);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConnectException) {
                // pass
            } else {
                fail("Unexpected cause: " + e);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static void assertRedirect(String expectedURI, String uri) {
        try {
            HttpClient httpClient = newClient();
            Request request = httpClient.newRequest(uri);
            request.method("GET");
            ContentResponse response = request.send();
            assertEquals(expectedURI, response.getHeaders().get("Location"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static void assertRepresentationContains(String contains,
                                                    String uri) {
        try {
            HttpClient httpClient = newClient();
            Request request = httpClient.newRequest(uri);
            request.method("GET");
            ContentResponse response = request.send();
            assertTrue(response.getContentAsString().contains(contains));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static void assertStatus(int expectedCode, String uri) {
        try {
            HttpClient httpClient = newClient();
            Request request = httpClient.newRequest(uri);
            request.method("GET");
            ContentResponse response = request.send();
            assertEquals(expectedCode, response.getStatus());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private static HttpClient newClient() throws Exception {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        HttpClient httpClient = new HttpClient(sslContextFactory);
        httpClient.setFollowRedirects(false);
        httpClient.start();
        return httpClient;
    }

    private HTTPAssert() {}

}
