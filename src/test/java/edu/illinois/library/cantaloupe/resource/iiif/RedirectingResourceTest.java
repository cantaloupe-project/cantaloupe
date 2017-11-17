package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional test of RedirectingResource.
 */
public class RedirectingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_PATH;
    }

    @Test
    public void testDoGet() throws Exception {
        client = newClient("");
        ContentResponse response = client.send();

        assertEquals(303, response.getStatus());
        assertTrue(response.getHeaders().get("Location").
                endsWith(RestletApplication.IIIF_2_PATH));
        assertTrue(response.getContentAsString().isEmpty());
    }

}
