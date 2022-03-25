package edu.illinois.library.cantaloupe.test.Assert;

import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;

import java.net.ConnectException;
import java.net.URI;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public final class HTTPAssert {

    public static void assertConnectionRefused(String uri) {
        Client client = newClient();
        try {
            client.setURI(new URI(uri));
            client.send();
            fail("Connection not refused: " + uri);
        } catch (ConnectException e) {
            // pass
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRedirect(URI fromURI, URI toURI, int status) {
        Client client = newClient();
        try {
            client.setURI(fromURI);
            Response response = client.send();
            assertEquals(toURI.toString(),
                    response.getHeaders().getFirstValue("Location"));
            assertEquals(status, response.getStatus());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRepresentationContains(String contains,
                                                    String uri) {
        Client client = newClient();
        try {
            client.setURI(new URI(uri));
            Response response = client.send();
            assertTrue(response.getBodyAsString().contains(contains));
        } catch (ResourceException e) {
            assertTrue(e.getResponse().getBodyAsString().contains(contains));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRepresentationContains(String contains, URI uri) {
        assertRepresentationContains(contains, uri.toString());
    }

    public static void assertRepresentationEquals(String expected,
                                                  String uri) {
        Client client = newClient();
        try {
            client.setURI(new URI(uri));
            Response response = client.send();
            assertEquals(expected, response.getBodyAsString());
        } catch (ResourceException e) {
            assertEquals(expected, e.getResponse().getBodyAsString());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRepresentationEquals(String equals, URI uri) {
        assertRepresentationEquals(equals, uri.toString());
    }

    public static void assertRepresentationsNotSame(URI uri1, URI uri2) {
        Client client = newClient();
        try {
            client.setURI(uri1);
            Response response = client.send();
            byte[] body1 = response.getBody();

            client.setURI(uri2);
            response = client.send();
            byte[] body2 = response.getBody();

            assertFalse(Arrays.equals(body1, body2));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertStatus(int expectedCode, String uri) {
        Client client = newClient();
        try {
            client.setURI(new URI(uri));
            Response response = client.send();
            assertEquals(expectedCode, response.getStatus());
        } catch (ResourceException e) {
            assertEquals(expectedCode, e.getStatusCode());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertStatus(int expectedCode, URI uri) {
        assertStatus(expectedCode, uri.toString());
    }

    private static Client newClient() {
        Client client = new Client();
        client.setTrustAll(true);
        client.setFollowRedirects(false);
        return client;
    }

    private static void stopQuietly(Client client) {
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    private HTTPAssert() {}

}
