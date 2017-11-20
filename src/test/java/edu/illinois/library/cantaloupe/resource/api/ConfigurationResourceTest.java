package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.MediaType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of ConfigurationResource.
 */
public class ConfigurationResourceTest extends AbstractAPIResourceTest {

    @Override
    protected String getEndpointPath() {
        return RestletApplication.CONFIGURATION_PATH;
    }

    @Override
    @Test
    public void testEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);

        try {
            client.setMethod(Method.GET);
            Response r = client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    /* getConfiguration() */

    @Test
    public void testGetConfiguration() throws Exception {
        System.setProperty("cats", "yes");

        client.setMethod(Method.GET);
        Response response = client.send();
        assertTrue(response.getBodyAsString().startsWith("{"));
    }

    /* putConfiguration() */

    @Test
    public void testPutConfiguration() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        client.setMethod(Method.PUT);
        client.setContentType(MediaType.APPLICATION_JSON);
        client.setEntity(entity);
        client.send();

        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

}
