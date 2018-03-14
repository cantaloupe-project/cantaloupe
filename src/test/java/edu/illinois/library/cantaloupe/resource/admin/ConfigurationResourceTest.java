package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
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

public class ConfigurationResourceTest extends AbstractAdminResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.ADMIN_CONFIG_PATH;
    }

    @Test
    public void testGETWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty("test", "cats");

        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGETWhenDisabled() throws Exception {
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
    public void testGETResponseHeaders() throws Exception {
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(7, headers.size());

        // Cache-Control
        assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8",
                headers.getFirstValue("Content-Type"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertTrue(headers.getFirstValue("Server").contains("Restlet"));
        // Transfer-Encoding
        assertEquals("chunked", headers.getFirstValue("Transfer-Encoding"));
        // Vary
        List<String> parts =
                Arrays.asList(StringUtils.split(headers.getFirstValue("Vary"), ", "));
        assertEquals(5, parts.size());
        assertTrue(parts.contains("Accept"));
        assertTrue(parts.contains("Accept-Charset"));
        assertTrue(parts.contains("Accept-Encoding"));
        assertTrue(parts.contains("Accept-Language"));
        assertTrue(parts.contains("Origin"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

    @Test
    public void testGETResponseBody() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty("test", "cats");

        Response response = client.send();
        assertTrue(response.getBodyAsString().contains("\"test\":\"cats\""));
    }

    @Override
    @Test
    public void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, true);

        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods =
                Arrays.asList(StringUtils.split(headers.getFirstValue("Allow"), ", "));
        assertEquals(3, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    public void testPUTWhenEnabled() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.setEntity(entityStr);
        client.setContentType(new MediaType("application/json"));
        client.send();

        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

    @Test
    public void testPUTWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, false);

        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().
                writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.setEntity(entityStr);
        client.setContentType(new MediaType("application/json"));

        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
