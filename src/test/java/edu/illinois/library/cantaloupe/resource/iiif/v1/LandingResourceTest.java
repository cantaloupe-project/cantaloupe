package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Test;

import java.net.URI;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_1_PATH;
    }

    @Test
    public void testWithEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);
        assertStatus(200, getHTTPURI(""));
        assertRepresentationContains("Cantaloupe Image", getHTTPURI(""));
    }

    @Test
    public void testWithEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, false);
        assertStatus(403, getHTTPURI(""));
    }

    @Test
    public void testGetWithTrailingSlashRedirectsToWithout() throws Exception {
        final URI uri = getHTTPURI("");
        assertRedirect(new URI(uri + "/"), uri, 301);
    }

}
