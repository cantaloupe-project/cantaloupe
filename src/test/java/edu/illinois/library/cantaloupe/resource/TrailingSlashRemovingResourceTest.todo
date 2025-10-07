package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrailingSlashRemovingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return Route.IIIF_2_PATH;
    }

    @Test
    void testDoGet() throws Exception {
        client = newClient("/");
        Response response = client.send();

        assertEquals(301, response.getStatus());
        assertFalse(response.getHeaders().getFirstValue("Location").endsWith("/"));
        assertTrue(response.getBodyAsString().isEmpty());
    }

    @Test
    void testDoGetRespectsBaseURIConfigKey() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/cats");

        client = newClient("/");
        Response response = client.send();

        assertEquals(301, response.getStatus());
        assertTrue(response.getHeaders().getFirstValue("Location").
                endsWith("/cats" + getEndpointPath()));
        assertTrue(response.getBodyAsString().isEmpty());
    }

    @Test
    void testDoGetRespectsXForwardedHeaders() throws Exception {
        client = newClient("/");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Port", "80");
        Response response = client.send();

        assertEquals(301, response.getStatus());
        assertTrue(response.getHeaders().getFirstValue("Location").
                endsWith(getEndpointPath()));
        assertTrue(response.getBodyAsString().isEmpty());
    }

}
