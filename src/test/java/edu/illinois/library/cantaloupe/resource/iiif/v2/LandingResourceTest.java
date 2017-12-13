package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_2_PATH;
    }

    @Test
    public void testGETWithEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);
        assertStatus(200, getHTTPURI(""));
        assertRepresentationContains("Cantaloupe Image", getHTTPURI(""));
    }

    @Test
    public void testGETWithEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        assertStatus(403, getHTTPURI(""));
    }

    @Test
    public void testGETWithTrailingSlashRedirectsToWithout() throws Exception {
        final URI uri = getHTTPURI("");
        assertRedirect(new URI(uri + "/"), uri, 301);
    }

    @Test
    public void testGETResponseHeaders() throws Exception {
        client = newClient("");
        Response response = client.send();
        Map<String,String> headers = response.getHeaders();
        assertEquals(7, headers.size());

        // Accept-Ranges
        assertEquals("bytes", headers.get("Accept-Ranges"));
        // Content-Type
        assertEquals("text/html;charset=UTF-8", headers.get("Content-Type"));
        // Date
        assertNotNull(headers.get("Date"));
        // Server
        assertTrue(headers.get("Server").contains("Restlet"));
        // Transfer-Encoding
        assertEquals("chunked", headers.get("Transfer-Encoding"));
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

    @Test
    public void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        client = newClient("");
        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Map<String,String> headers = response.getHeaders();
        List<String> methods = Arrays.asList(StringUtils.split(headers.get("Allow"), ", "));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    public void testOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        try {
            client = newClient("");
            client.setMethod(Method.OPTIONS);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
