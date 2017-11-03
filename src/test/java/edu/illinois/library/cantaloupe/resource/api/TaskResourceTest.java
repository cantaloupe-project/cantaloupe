package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;

/**
 * Functional test of TaskResource.
 */
public class TaskResourceTest extends AbstractAPIResourceTest {

    @Override
    String getURIPath() {
        return RestletApplication.TASKS_PATH + "/some-uuid";
    }

    /* doGet() */

    @Test
    public void testDoGetWithInvalidID() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        try {
            client.get();
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_NOT_FOUND, e.getStatus());
        }
    }

    @Test
    public void testDoGetWithValidID() throws Exception {
        // Create a task
        APITask<?> submittedTask =
                new APITask<>(new PurgeInvalidFromCacheCommand<>());
        // Get its JSON representation
        Representation submittedRep = new JSONRepresentation(submittedTask);

        // Submit it to TasksResource
        String tasksURIPath = RestletApplication.TASKS_PATH;
        ClientResource client = getClientForUriPath(
                tasksURIPath, USERNAME, SECRET);
        client.post(submittedRep, MediaType.APPLICATION_JSON);

        // Retrieve it by its UUID from TaskResource
        Reference location = client.getResponse().getLocationRef();
        client = getClientForUriPath(location.getPath(), USERNAME, SECRET);
        Representation rep = client.get();

        assertEquals(Status.SUCCESS_OK, client.getStatus());
        String responseBody = rep.getText();

        assertTrue(responseBody.contains("PurgeInvalidFromCache"));
    }

}
