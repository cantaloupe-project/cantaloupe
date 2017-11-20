package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

/**
 * Functional test of TaskResource.
 */
public class TaskResourceTest extends AbstractAPIResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.TASKS_PATH + "/some-uuid";
    }

    /* doGet() */

    @Test
    public void testDoGetWithInvalidID() throws Exception {
        try {
            client.setMethod(Method.GET);
            client.send();
        } catch (ResourceException e) {
            assertEquals(404, e.getStatusCode());
        }
    }

    @Test
    public void testDoGetWithValidID() throws Exception {
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
        String location = response.getHeaders().get("Location");
        client.setURI(new URI(location));
        client.setMethod(Method.GET);
        response = client.send();

        assertEquals(200, response.getStatus());
        String responseBody = response.getBodyAsString();

        assertTrue(responseBody.contains("PurgeInvalidFromCache"));
    }

}
