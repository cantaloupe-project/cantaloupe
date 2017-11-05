package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;

/**
 * Functional test of TasksResource.
 */
public class TasksResourceTest extends AbstractAPIResourceTest {

    @Override
    String getURIPath() {
        return RestletApplication.TASKS_PATH;
    }

    /* doPost() */

    @Test
    public void testDoPostWithIncorrectContentType() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation(
                "{ \"verb\": \"PurgeCache\" }", MediaType.TEXT_PLAIN);
        try {
            client.post(rep, MediaType.TEXT_PLAIN);
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
                    e.getStatus());
        }
    }

    @Test
    public void testDoPostWithMalformedRequestBody() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation("{ this is: invalid\" }",
                MediaType.APPLICATION_JSON);
        try {
            client.post(rep);
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void testDoPostWithMissingVerb() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation("{ \"cats\": \"yes\" }",
                MediaType.APPLICATION_JSON);
        try {
            client.post(rep);
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void testDoPostWithUnsupportedVerb() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation("{ \"verb\": \"dogs\" }",
                MediaType.APPLICATION_JSON);
        try {
            client.post(rep);
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void testDoPostWithPurgeDelegateMethodInvocationCacheVerb() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation(
                "{ \"verb\": \"PurgeDelegateMethodInvocationCache\" }",
                MediaType.APPLICATION_JSON);

        client.post(rep);
        assertEquals(Status.SUCCESS_ACCEPTED, client.getStatus());
        assertNotNull(client.getLocationRef());
    }

    @Test
    public void testDoPostWithPurgeInvalidFromCacheVerb() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation(
                "{ \"verb\": \"PurgeInvalidFromCache\" }",
                MediaType.APPLICATION_JSON);

        client.post(rep);
        assertEquals(Status.SUCCESS_ACCEPTED, client.getStatus());
        assertNotNull(client.getLocationRef());
    }

    @Test
    public void testDoPostWithPurgeItemFromCacheVerb() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        Representation rep = new StringRepresentation(
                "{ \"verb\": \"PurgeItemFromCache\", \"identifier\": \"cats\" }",
                MediaType.APPLICATION_JSON);

        client.post(rep);
        assertEquals(Status.SUCCESS_ACCEPTED, client.getStatus());
        assertNotNull(client.getLocationRef());
    }

}
