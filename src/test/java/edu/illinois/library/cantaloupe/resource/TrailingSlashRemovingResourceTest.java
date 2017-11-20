package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Response;
import org.junit.Test;

import static org.junit.Assert.*;

public class TrailingSlashRemovingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_2_PATH;
    }

    @Test
    public void testDoGet() throws Exception {
        client = newClient("/");
        Response response = client.send();

        assertEquals(301, response.getStatus());
        assertFalse(response.getHeaders().get("Location").endsWith("/"));
        assertTrue(response.getBodyAsString().isEmpty());
    }

    @Test
    public void testDoGetRespectsBaseURIConfigKey() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/cats");

        client = newClient("/");
        Response response = client.send();

        assertEquals(301, response.getStatus());
        assertTrue(response.getHeaders().get("Location").
                endsWith("/cats" + getEndpointPath()));
        assertTrue(response.getBodyAsString().isEmpty());
    }

    @Test
    public void testDoGetRespectsXForwardedHeaders() throws Exception {
        client = newClient("/");
        client.getHeaders().put("X-Forwarded-Host", "http://example.org/");
        client.getHeaders().put("X-Forwarded-Proto", "HTTP");
        client.getHeaders().put("X-Forwarded-Port", "80");
        client.getHeaders().put("X-Forwarded-Path", "/cats");
        Response response = client.send();

        assertEquals(301, response.getStatus());
        assertTrue(response.getHeaders().get("Location").
                endsWith("/cats" + getEndpointPath()));
        assertTrue(response.getBodyAsString().isEmpty());
    }

}
