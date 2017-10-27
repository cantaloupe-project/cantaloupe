package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

import static org.junit.Assert.*;

/**
 * Functional test of CacheResource.
 */
public class CacheResourceTest extends AbstractAPIResourceTest {

    private static final String IDENTIFIER = "jpg";

    @Override
    String getURIPath() {
        return RestletApplication.CACHE_PATH + "/" + IDENTIFIER;
    }

    /* doPurge() */

    @Test
    public void testDoPurgeWithValidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(
                RestletApplication.CACHE_PATH + "/" + IDENTIFIER, USERNAME, SECRET);
        client.delete();
        assertEquals(Status.SUCCESS_NO_CONTENT, client.getStatus());

        // TODO: assert that relevant cache files have been deleted
    }

}
