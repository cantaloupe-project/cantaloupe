package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

import java.io.IOException;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    public void testGet() throws IOException {
        ClientResource client = getClientForUriPath(
                ImageServerApplication.IIIF_2_0_PATH);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.get().getText().contains("Cantaloupe Image"));
    }

}
