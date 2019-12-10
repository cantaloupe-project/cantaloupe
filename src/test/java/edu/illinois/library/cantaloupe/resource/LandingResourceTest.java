package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return "";
    }

    @Test
    void testGET() {
        assertStatus(200, getHTTPURI(""));
    }

    @Test
    void testGETResponseBody() {
        assertRepresentationContains("<body", getHTTPURI(""));
    }

    @Test
    void testGETResponseHeaders() throws Exception {
        client = newClient("");
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(6, headers.size());

        // Cache-Control
        assertTrue(headers.getFirstValue("Cache-Control").contains("public"));
        assertTrue(headers.getFirstValue("Cache-Control").contains("max-age="));
        // Content-Type
        assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
                headers.getFirstValue("Content-Type")));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

    @Test
    void testOPTIONS() throws Exception {
        client = newClient("");
        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods = List.of(headers.getFirstValue("Allow").split(","));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("OPTIONS"));
    }

}
