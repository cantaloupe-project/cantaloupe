package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigurationResourceTest extends ResourceTest {

    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_SECRET, SECRET);

        client = newClient("", USERNAME, SECRET,
                RestletApplication.ADMIN_REALM);
    }

    @Override
    protected String getEndpointPath() {
        return RestletApplication.ADMIN_CONFIG_PATH;
    }

    @Test
    public void testGetConfiguration() throws Exception {
        Configuration.getInstance().setProperty("test", "cats");

        Response response = client.send();

        assertTrue(response.getBodyAsString().contains("\"test\":\"cats\""));
    }

    @Test
    public void testPutConfiguration() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.setEntity(entityStr);
        client.setContentType(new MediaType("application/json"));
        client.send();

        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

    @Test
    public void testEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        // enabled
        config.setProperty(AbstractAdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, true);

        Response response = client.send();
        assertEquals(200, response.getStatus());

        // disabled
        config.setProperty(AbstractAdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, false);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
