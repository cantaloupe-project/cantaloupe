package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of ConfigurationResource.
 */
public class ConfigurationResourceTest extends AbstractAPIResourceTest {

    @Override
    String getURIPath() {
        return RestletApplication.CONFIGURATION_PATH;
    }

    @Override
    @Test
    @Ignore // TODO: why does super fail?
    public void testEndpointDisabled() {}

    /* getConfiguration() */

    @Test
    public void testGetConfiguration() throws Exception {
        System.setProperty("cats", "yes");

        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        client.get();

        assertTrue(client.getResponseEntity().getText().startsWith("{"));
    }

    /* putConfiguration() */

    @Test
    @Ignore // TODO: why does this fail?
    public void testPutConfiguration() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        client.put(entity, MediaType.APPLICATION_JSON);

        assertEquals("cats", Configuration.getInstance().getString("test"));
    }

}
