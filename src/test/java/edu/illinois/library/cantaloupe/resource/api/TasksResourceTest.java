package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of TasksResource.
 */
public class TasksResourceTest extends AbstractAPIResourceTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        client.setMethod(Method.POST);
    }

    @Override
    protected String getEndpointPath() {
        return RestletApplication.TASKS_PATH;
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
        assertTrue(methods.contains("POST"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    public void testPOSTWithIncorrectContentType() throws Exception {
        try {
            client.setEntity("{ \"verb\": \"PurgeCache\" }");
            client.setContentType(MediaType.TEXT_PLAIN);
            client.send();
        } catch (ResourceException e) {
            assertEquals(415, e.getStatusCode());
        }
    }

    @Test
    public void testPOSTWithEmptyRequestBody() throws Exception {
        try {
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testPOSTWithMalformedRequestBody() throws Exception {
        try {
            client.setEntity("{ this is: invalid\" }");
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testPOSTWithMissingVerb() throws Exception {
        try {
            client.setEntity("{ \"cats\": \"yes\" }");
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testPOSTWithUnsupportedVerb() throws Exception {
        try {
            client.setEntity("{ \"verb\": \"dogs\" }");
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testPOSTWithPurgeDelegateMethodInvocationCacheVerb()
            throws Exception {
        client.setEntity("{ \"verb\": \"PurgeDelegateMethodInvocationCache\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();

        assertEquals(202, response.getStatus());
        assertNotNull(response.getHeaders().get("Location"));
    }

    @Test
    public void testPOSTWithPurgeInvalidFromCacheVerb() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeInvalidFromCache\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();

        assertEquals(202, response.getStatus());
        assertNotNull(response.getHeaders().get("Location"));
    }

    @Test
    public void testPOSTWithPurgeItemFromCacheVerb() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeItemFromCache\", \"identifier\": \"cats\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();

        assertEquals(202, response.getStatus());
        assertNotNull(response.getHeaders().get("Location"));
    }

    @Test
    public void testPOSTResponseHeaders() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeDelegateMethodInvocationCache\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();
        Map<String,String> headers = response.getHeaders();
        assertEquals(9, headers.size());

        // Accept-Ranges
        assertEquals("bytes", headers.get("Accept-Ranges"));
        // Cache-Control
        assertEquals("no-cache", headers.get("Cache-Control"));
        // Content-Length
        assertEquals("0", headers.get("Content-Length"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8", headers.get("Content-Type"));
        // Date
        assertNotNull(headers.get("Date"));
        // Location
        assertNotNull(headers.get("Location"));
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

}
