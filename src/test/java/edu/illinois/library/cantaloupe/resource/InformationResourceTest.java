package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.restlet.data.CacheDirective;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    public void testCacheHeaders() {
        Configuration config = Application.getConfiguration();
        config.setProperty("cache.client.max_age", "1234");
        config.setProperty("cache.client.shared_max_age", "4567");
        config.setProperty("cache.client.public", "false");
        config.setProperty("cache.client.private", "true");
        config.setProperty("cache.client.no_cache", "true");
        config.setProperty("cache.client.no_store", "true");
        config.setProperty("cache.client.must_revalidate", "false");
        config.setProperty("cache.client.proxy_revalidate", "true");
        config.setProperty("cache.client.no_transform", "false");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("max-age", "1234");
        expectedDirectives.put("s-maxage", "4567");
        expectedDirectives.put("private", null);
        expectedDirectives.put("no-cache", null);
        expectedDirectives.put("no-store", null);
        expectedDirectives.put("proxy-revalidate", null);
        expectedDirectives.put("no-transform", null);

        ClientResource client = getClientForUriPath("/jpg/info.json");
        client.get();
        List<CacheDirective> actualDirectives = client.getResponse().getCacheDirectives();
        for (CacheDirective d : actualDirectives) {
            if (d.getName() != null) {
                assertTrue(expectedDirectives.keySet().contains(d.getName()));
                if (d.getValue() != null) {
                    assertTrue(expectedDirectives.get(d.getName()).equals(d.getValue()));
                } else {
                    assertNull(expectedDirectives.get(d.getName()));
                }
            }
        }
    }

    public void testNotFound() throws IOException {
        ClientResource client = getClientForUriPath("/invalid/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_NOT_FOUND, client.getStatus());
        }
    }

    public void testUnavailableSourceFormat() throws IOException {
        ClientResource client = getClientForUriPath("/text.txt/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
                    client.getStatus());
        }
    }

}
