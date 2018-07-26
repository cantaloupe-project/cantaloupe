package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.resource.Route;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

/**
 * Functional test of TaskResource.
 */
public class TaskResourceTest extends AbstractAPIResourceTest {

    @Override
    protected String getEndpointPath() {
        return Route.TASKS_PATH + "/some-uuid";
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
        assertEquals(6, headers.size());

        // Cache-Control
        assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8",
                headers.getFirstValue("Content-Type"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
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
                Route.TASKS_PATH));
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
