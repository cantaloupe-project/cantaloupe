package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StatusResourceTest extends ResourceTest {

    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_USERNAME, USERNAME);
        config.setProperty(Key.ADMIN_SECRET, SECRET);

        client = newClient("", USERNAME, SECRET,
                RestletApplication.ADMIN_REALM);
    }

    @Override
    protected String getEndpointPath() {
        return RestletApplication.ADMIN_STATUS_PATH;
    }

    @Test
    public void testGet() throws Exception {
        Response response = client.send();

        assertTrue(response.getBodyAsString().contains("\"infoCache\":"));
    }

    @Test
    public void testEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, true);

        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, false);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    @Test
    public void testResponseHeaders() throws Exception {
        Response response = client.send();
        Map<String,String> headers = response.getHeaders();
        assertEquals(8, headers.size());

        // Accept-Ranges
        assertEquals("bytes", headers.get("Accept-Ranges")); // TODO: remove this
        // Cache-Control
        assertEquals("no-cache", headers.get("Cache-Control"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8", headers.get("Content-Type"));
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

}
