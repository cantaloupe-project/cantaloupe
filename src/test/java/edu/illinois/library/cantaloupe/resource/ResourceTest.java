package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ResourceTest extends TestCase {

    protected static final Integer PORT = 34852;

    protected static Client client = new Client(new Context(), Protocol.HTTP);

    public static BaseConfiguration newConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path fixturePath = Paths.get(cwd, "src", "test", "resources");
            config.setProperty("print_stack_trace_on_error_pages", false);
            config.setProperty("http.port", PORT);
            config.setProperty("processor.fallback", "Java2dProcessor");
            config.setProperty("resolver", "FilesystemResolver");
            config.setProperty("FilesystemResolver.path_prefix", fixturePath + File.separator);
        } catch (Exception e) {
            fail("Failed to get the configuration");
        }
        return config;
    }

    public void setUp() throws Exception {
        Application.setConfiguration(newConfiguration());
        Application.startServer();
    }

    public void tearDown() throws Exception {
        Application.stopServer();
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

    public void testBasicAuth() {
        /* TODO: write this
        Configuration config = Application.getConfiguration();
        config.setProperty("http.auth.basic", "true");
        config.setProperty("http.auth.basic.username", "user");
        config.setProperty("http.auth.basic.secret", "pass");

        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");
        client.get();
        */
    }

}
