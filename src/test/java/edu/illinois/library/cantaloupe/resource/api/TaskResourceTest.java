package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Functional test of TaskResource.
 */
public class TaskResourceTest extends AbstractAPIResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.TASKS_PATH + "/some-uuid";
    }

    @Test
    public void testGETWithInvalidID() throws Exception {
        try {
            client.setMethod(Method.GET);
            client.send();
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        }
    }

    @Test
    public void testGETWithValidID() throws Exception {
        Response response = createTask();

        assertEquals(200, response.getStatus());
        String responseBody = response.getBodyAsString();

        assertTrue(responseBody.contains("PurgeInvalidFromCache"));
    }

    @Test
    public void testGETResponseHeaders() throws Exception {
        Response response = createTask();

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
        assertEquals("chunked",
                headers.getFirstValue("Transfer-Encoding"));
        // Vary
        List<String> parts =
                Arrays.asList(StringUtils.split(headers.getFirstValue("Vary"), ", "));
        assertEquals(4, parts.size());
        //assertTrue(parts.contains("Accept")); // TODO: why is this missing?
        assertTrue(parts.contains("Accept-Charset"));
        assertTrue(parts.contains("Accept-Encoding"));
        assertTrue(parts.contains("Accept-Language"));
        assertTrue(parts.contains("Origin"));
        // X-Powered-By
        assertEquals(Application.NAME + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

    private Response createTask() throws Exception {
        // Create a task
        APITask<?> submittedTask =
                new APITask<>(new PurgeInvalidFromCacheCommand<>());
        // Get its JSON representation
        String entityStr = new ObjectMapper().writer().
                writeValueAsString(submittedTask);

        // Submit it to TasksResource
        client.setMethod(Method.POST);
        client.setURI(new URI("http://localhost:" + appServer.getHTTPPort() +
                RestletApplication.TASKS_PATH));
        client.setEntity(entityStr);
        client.setContentType(new MediaType("application/json"));
        Response response = client.send();

        // Retrieve it by its UUID from TaskResource
        String location = response.getHeaders().getFirstValue("Location");
        client.setURI(new URI(location));
        client.setMethod(Method.GET);

        return client.send();
    }

}
