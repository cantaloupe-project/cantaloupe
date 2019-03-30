package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.resource.Route;
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
        return Route.CONFIGURATION_PATH;
    }

    @Test
    public void testGETWithEndpointEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        client.setMethod(Method.GET);
        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGETWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);

        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    @Test
    public void testGETResponseBody() throws Exception {
        client.setMethod(Method.GET);
        Response response = client.send();
        assertTrue(response.getBodyAsString().startsWith("{"));
    }

    @Test
    public void testGETResponseHeaders() throws Exception {
        client.setMethod(Method.GET);
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(6, headers.size());

        // Cache-Control
        assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8",
                headers.getFirstValue("Content-Type"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

    @Test
    public void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods =
                List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
        assertEquals(3, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    public void testOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);
        try {
            client.setMethod(Method.OPTIONS);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    @Test
    public void testPUT() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.setContentType(MediaType.APPLICATION_JSON);
        client.setEntity(entity);
        client.send();

        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

}
