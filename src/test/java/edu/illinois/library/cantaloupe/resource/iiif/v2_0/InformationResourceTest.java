package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
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

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(ImageServerApplication.IIIF_2_0_PATH + path);
    }

    public void testCacheHeaders() {
        Configuration config = Application.getConfiguration();
        config.setProperty("cache.client.enabled", "true");
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

    public void testJson() throws IOException {
        // TODO: this could be a lot more thorough; but the aspects of the JSON
        // response defined in the Image API spec are tested in
        // ConformanceTest

        // test whether the @id property respects the generate_https_links
        // setting in the app config
        ClientResource client = getClientForUriPath("/escher_lego.jpg/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertTrue(info.getId().startsWith("http://"));

        Configuration config = Application.getConfiguration();
        config.setProperty("generate_https_links", true);
        client.get();
        json = client.getResponse().getEntityAsText();
        info = mapper.readValue(json, ImageInfo.class);
        assertTrue(info.getId().startsWith("https://"));
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
