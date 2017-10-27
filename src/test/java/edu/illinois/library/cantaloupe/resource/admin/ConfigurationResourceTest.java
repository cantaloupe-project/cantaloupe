package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigurationResourceTest extends ResourceTest {

    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    private ConfigurationResource instance;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_SECRET, SECRET);

        instance = new ConfigurationResource();
        Request request = new Request();
        instance.init(new Context(), request, new Response(request));
    }

    @Test
    public void testGetConfigurationAsJson() throws Exception {
        Configuration.getInstance().setProperty("test", "cats");

        Representation rep = instance.getConfiguration();

        assertTrue(rep.getText().contains("\"test\":\"cats\""));
    }

    @Test
    public void testPutConfiguration() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);
        Representation rep = new StringRepresentation(
                entityStr, MediaType.APPLICATION_JSON);

        instance.putConfiguration(rep);


        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

    @Test
    public void testEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        // enabled
        config.setProperty(AbstractAdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, true);

        ClientResource client = getClientForUriPath(
                RestletApplication.ADMIN_CONFIG_PATH, USERNAME, SECRET);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        // disabled
        config.setProperty(AbstractAdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

}
