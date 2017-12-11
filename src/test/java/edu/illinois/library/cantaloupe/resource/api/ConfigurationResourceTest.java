package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of ConfigurationResource.
 */
public class ConfigurationResourceTest extends AbstractAPIResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.CONFIGURATION_PATH;
    }

    @Override
    @Test
    public void testEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);

        try {
            client.setMethod(Method.GET);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    /* getConfiguration() */

    @Test
    public void testGetConfiguration() throws Exception {
        System.setProperty("cats", "yes");

        client.setMethod(Method.GET);
        Response response = client.send();
        assertTrue(response.getBodyAsString().startsWith("{"));
    }

    /* putConfiguration() */

    @Test
    public void testPutConfiguration() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.setContentType(MediaType.APPLICATION_JSON);
        client.setEntity(entity);
        client.send();

        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

    @Test
    public void testResponseHeaders() throws Exception {
        client.setMethod(Method.GET);
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
