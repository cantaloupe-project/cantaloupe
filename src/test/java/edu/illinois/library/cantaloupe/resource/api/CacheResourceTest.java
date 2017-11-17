package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.http.Method;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional test of CacheResource.
 */
public class CacheResourceTest extends AbstractAPIResourceTest {

    private static final String IDENTIFIER = "jpg";

    @Override
    protected String getEndpointPath() {
        return RestletApplication.CACHE_PATH + "/" + IDENTIFIER;
    }

    /* doPurge() */

    @Test
    public void testDoPurgeWithValidCredentials() throws Exception {
        client.setMethod(Method.DELETE);
        ContentResponse response = client.send();
        assertEquals(204, response.getStatus());

        // TODO: assert that relevant cache files have been deleted
    }

}
