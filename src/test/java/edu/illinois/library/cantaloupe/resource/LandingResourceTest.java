package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
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
    public void testGET() {
        assertStatus(200, getHTTPURI(""));
    }

    @Test
    public void testGETResponseBody() {
        assertRepresentationContains("<body", getHTTPURI(""));
    }

    @Test
    public void testGETResponseHeaders() throws Exception {
        client = newClient("");
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(7, headers.size());

        // Cache-Control
        assertTrue(headers.getFirstValue("Cache-Control").contains("public"));
        assertTrue(headers.getFirstValue("Cache-Control").contains("max-age="));
        // Content-Type
        assertEquals("text/html;charset=UTF-8",
                headers.getFirstValue("Content-Type"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertTrue(headers.getFirstValue("Server").contains("Restlet"));
        // Transfer-Encoding
        assertEquals("chunked",
                headers.getFirstValue("Transfer-Encoding"));
        // Vary
        List<String> parts =
                Arrays.asList(StringUtils.split(headers.getFirstValue("Vary"), ", "));
        assertEquals(5, parts.size());
        assertTrue(parts.contains("Accept"));
        assertTrue(parts.contains("Accept-Charset"));
        assertTrue(parts.contains("Accept-Encoding"));
        assertTrue(parts.contains("Accept-Language"));
        assertTrue(parts.contains("Origin"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

    @Test
    public void testOPTIONS() throws Exception {
        client = newClient("");
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

}
