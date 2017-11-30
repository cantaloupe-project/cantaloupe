package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StatusResourceTest extends ResourceTest {

    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_USERNAME, USERNAME);
        config.setProperty(Key.ADMIN_SECRET, SECRET);

        client = newClient("", USERNAME, SECRET,
                RestletApplication.ADMIN_REALM);
    }

    @Override
    protected String getEndpointPath() {
        return RestletApplication.ADMIN_STATUS_PATH;
    }

    @Test
    public void testGet() throws Exception {
        Response response = client.send();

        assertTrue(response.getBodyAsString().contains("\"infoCache\":"));
    }

    @Test
    public void testEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, true);

        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_ENABLED, false);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
