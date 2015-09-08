package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.AfterClass;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ResourceTest extends TestCase {

    private static final Integer PORT = 34852;

    private static Client client = new Client(new Context(), Protocol.HTTP);

    /**
     * Initializes the Restlet application
     */
    static { // TODO: why doesn't this code work in a @BeforeClass?
        try {
            Application.setConfiguration(getConfiguration());
            Application.start();
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

    @AfterClass
    public void afterClass() throws Exception {
        Application.stop();
    }

    public void setUp() {
        BaseConfiguration config = getConfiguration();
        Application.setConfiguration(config);
    }

    protected ClientResource getClientForUriPath(String path) {
        Reference url = new Reference(getBaseUri() + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

    protected String getBaseUri() {
        return "http://localhost:" + PORT +
                ImageServerApplication.BASE_IIIF_PATH;
    }

}
