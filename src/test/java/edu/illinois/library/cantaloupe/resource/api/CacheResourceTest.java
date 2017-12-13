package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of CacheResource.
 */
public class CacheResourceTest extends AbstractAPIResourceTest {

    private static final String IDENTIFIER = "jpg";

    @Override
    protected String getEndpointPath() {
        return RestletApplication.CACHE_PATH + "/" + IDENTIFIER;
    }

    @Test
    public void testDELETEWithEndpointEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        client.setMethod(Method.DELETE);
        Response response = client.send();
        assertEquals(204, response.getStatus());
    }

    @Test
    public void testDELETEWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);

        try {
            client.setMethod(Method.DELETE);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    @Test
    public void testDELETEPurgesTheCache() throws Exception {
        client.setMethod(Method.DELETE);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        // TODO: assert that relevant cache files have been deleted
    }

    @Test
    public void testDELETEResponseHeaders() throws Exception {
        client.setMethod(Method.DELETE);
        Response response = client.send();
        Map<String,String> headers = response.getHeaders();
        assertEquals(7, headers.size());

        // Accept-Ranges
        assertEquals("bytes", headers.get("Accept-Ranges"));
        // Cache-Control
        assertEquals("no-cache", headers.get("Cache-Control"));
        // Content-Type
        assertEquals("text/plain", headers.get("Content-Type"));
        // Date
        assertNotNull(headers.get("Date"));
        // Server
        assertTrue(headers.get("Server").contains("Restlet"));
        // Vary
        List<String> parts = Arrays.asList(StringUtils.split(headers.get("Vary"), ", "));
        assertEquals(5, parts.size());
        assertTrue(parts.contains("Accept"));
        assertTrue(parts.contains("Accept-Charset"));
        assertTrue(parts.contains("Accept-Encoding"));
        assertTrue(parts.contains("Accept-Language"));
        assertTrue(parts.contains("Origin"));
        // X-Powered-By
        assertEquals("Cantaloupe/Unknown", headers.get("X-Powered-By"));
    }

    @Override
    @Test
    public void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Map<String,String> headers = response.getHeaders();
        List<String> methods = Arrays.asList(StringUtils.split(headers.get("Allow"), ", "));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("DELETE"));
        assertTrue(methods.contains("OPTIONS"));
    }

}
