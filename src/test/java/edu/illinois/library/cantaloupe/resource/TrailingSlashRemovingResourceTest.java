package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import static org.junit.Assert.*;

public class TrailingSlashRemovingResourceTest extends ResourceTest {

    @Test
    public void testDoGet() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.IIIF_2_PATH + "/");
        client.setFollowingRedirects(false);
        Representation responseRep = client.get();

        assertEquals(Status.REDIRECTION_PERMANENT, client.getStatus());
        assertTrue(client.getLocationRef().toString().
                endsWith(WebApplication.IIIF_2_PATH));
        assertTrue(responseRep.isEmpty());
    }

    @Test
    public void testDoGetRespectsBaseURIConfigKey() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY, "http://example.org/cats");

        ClientResource client = getClientForUriPath(WebApplication.IIIF_2_PATH + "/");
        client.setFollowingRedirects(false);
        Representation responseRep = client.get();

        assertEquals(Status.REDIRECTION_PERMANENT, client.getStatus());
        assertTrue(client.getLocationRef().toString().
                endsWith("/cats" + WebApplication.IIIF_2_PATH));
        assertTrue(responseRep.isEmpty());
    }

    @Test
    public void testDoGetRespectsXForwardedHeaders() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.IIIF_2_PATH + "/");
        client.setFollowingRedirects(false);
        client.getRequest().getHeaders().add("X-Forwarded-Host", "http://example.org/");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "80");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats");
        Representation responseRep = client.get();

        assertEquals(Status.REDIRECTION_PERMANENT, client.getStatus());
        assertTrue(client.getLocationRef().toString().
                endsWith("/cats" + WebApplication.IIIF_2_PATH));
        assertTrue(responseRep.isEmpty());
    }

}
