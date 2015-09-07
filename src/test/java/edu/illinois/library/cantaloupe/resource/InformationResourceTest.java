package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends TestCase {

    private static final String IMAGE = "escher_lego.jpg";
    private static final Integer PORT = 34852;

    private static Client client = new Client(new Context(), Protocol.HTTP);

    /**
     * Initializes the Restlet application
     */
    static {
        try {
            Application.setConfiguration(getConfiguration());
            Application.startRestlet();
        } catch (Exception e) {
            fail("Failed to start the Restlet");
        }
    }

    public static BaseConfiguration getConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path fixturePath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test", "fixtures");
            config.setProperty("print_stack_trace_on_error_pages", false);
            config.setProperty("http.port", PORT);
            config.setProperty("processor.fallback", "ImageIoProcessor");
            config.setProperty("resolver", "FilesystemResolver");
            config.setProperty("FilesystemResolver.path_prefix", fixturePath + File.separator);
        } catch (Exception e) {
            fail("Failed to get the configuration");
        }
        return config;
    }

    public void setUp() {
        BaseConfiguration config = getConfiguration();
        Application.setConfiguration(config);
    }

    public void testRootUri() throws IOException {
        ClientResource client = getClientForUriPath("");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.get().getText().contains("Cantaloupe IIIF 2.0 Server"));
    }

    public void testUnavailableSourceFormat() throws IOException {
        ClientResource client = getClientForUriPath("/text.txt/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    private ClientResource getClientForUriPath(String path) {
        Reference url = new Reference(getBaseUri() + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

    private String getBaseUri() {
        return "http://localhost:" + PORT +
                ImageServerApplication.BASE_IIIF_PATH;
    }

}
