package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

/**
 * Functional test of HTTP stuff happening outside of /iiif.
 */
public class RootResourceTest extends ResourceTest {

    public void testRootUriRedirectsToIiifUri() {
        // http://host:port -> /iiif
        Reference url = new Reference("http://localhost:" + PORT);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        resource.setFollowingRedirects(false);
        resource.get();

        assertEquals(Status.REDIRECTION_PERMANENT, resource.getStatus());
        assertEquals(getBaseUri(), resource.getLocationRef().toString());

        // http://host:port/ -> /iiif
        url = new Reference("http://localhost:" + PORT + "/");
        resource = new ClientResource(url);
        resource.setNext(client);
        resource.setFollowingRedirects(false);
        resource.get();

        assertEquals(Status.REDIRECTION_PERMANENT, resource.getStatus());
        assertEquals(getBaseUri(), resource.getLocationRef().toString());
    }

}
