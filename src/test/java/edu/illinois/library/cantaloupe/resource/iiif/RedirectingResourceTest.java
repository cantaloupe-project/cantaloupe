package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import static org.junit.Assert.*;

/**
 * Functional test of RedirectingResource.
 */
public class RedirectingResourceTest extends ResourceTest {

    @Test
    public void testDoGet() throws Exception {
        ClientResource client = getClientForUriPath(RestletApplication.IIIF_PATH);
        client.setFollowingRedirects(false);
        Representation responseRep = client.get();

        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertTrue(client.getLocationRef().toString().endsWith(RestletApplication.IIIF_2_PATH));
        assertTrue(responseRep.isEmpty());
    }

}
