package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

abstract class AbstractAPIResourceTest extends ResourceTest {

    static final String USERNAME = "admin";
    static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);
        config.setProperty(Key.API_USERNAME, USERNAME);
        config.setProperty(Key.API_SECRET, SECRET);

        client = newClient("", USERNAME, SECRET, RestletApplication.API_REALM);
    }

    @Test
    public void testGETWithNoCredentials() throws Exception {
        try {
            client.setUsername(null);
            client.setSecret(null);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testGETWithInvalidCredentials() throws Exception {
        client.setUsername("invalid");
        client.setSecret("invalid");
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods =
                Arrays.asList(StringUtils.split(headers.getFirstValue("Allow"), ", "));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    public void testOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);
        try {
            client.setMethod(Method.OPTIONS);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
