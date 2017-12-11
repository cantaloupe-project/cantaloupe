package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
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

    /* doPost() */

    @Test
    public void testDoPostWithIncorrectContentType() throws Exception {
        try {
            client.setEntity("{ \"verb\": \"PurgeCache\" }");
            client.setContentType(MediaType.TEXT_PLAIN);
            client.send();
        } catch (ResourceException e) {
            assertEquals(415, e.getStatusCode());
        }
    }

    @Test
    public void testDoPostWithEmptyRequestBody() throws Exception {
        try {
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testDoPostWithMalformedRequestBody() throws Exception {
        try {
            client.setEntity("{ this is: invalid\" }");
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testDoPostWithMissingVerb() throws Exception {
        try {
            client.setEntity("{ \"cats\": \"yes\" }");
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testDoPostWithUnsupportedVerb() throws Exception {
        try {
            client.setEntity("{ \"verb\": \"dogs\" }");
            client.setContentType(MediaType.APPLICATION_JSON);
            client.send();
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testDoPostWithPurgeDelegateMethodInvocationCacheVerb()
            throws Exception {
        client.setEntity("{ \"verb\": \"PurgeDelegateMethodInvocationCache\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();

        assertEquals(202, response.getStatus());
        assertNotNull(response.getHeaders().get("Location"));
    }

    @Test
    public void testDoPostWithPurgeInvalidFromCacheVerb() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeInvalidFromCache\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();

        assertEquals(202, response.getStatus());
        assertNotNull(response.getHeaders().get("Location"));
    }

    @Test
    public void testDoPostWithPurgeItemFromCacheVerb() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeItemFromCache\", \"identifier\": \"cats\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();

        assertEquals(202, response.getStatus());
        assertNotNull(response.getHeaders().get("Location"));
    }

    @Test
    public void testDoPostResponseHeaders() throws Exception {
        client.setEntity("{ \"verb\": \"PurgeDelegateMethodInvocationCache\" }");
        client.setContentType(MediaType.APPLICATION_JSON);
        Response response = client.send();
        Map<String,String> headers = response.getHeaders();
        assertEquals(9, headers.size());

        // Accept-Ranges TODO: remove this
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
