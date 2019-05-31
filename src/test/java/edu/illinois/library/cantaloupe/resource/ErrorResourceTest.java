package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.ResourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of error responses.
 */
public class ErrorResourceTest extends ResourceTest {

    @AfterEach
    public void tearDown() throws Exception {
        if (client != null) {
            client.stop();
        }
    }

    @Override
    protected String getEndpointPath() {
        return "";
    }

    @Test
    void testErrorResponseContentTypeWithHTMLPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        try {
            client.send();
        } catch (ResourceException e) {
            assertTrue(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void testErrorResponseContentTypeWithXHTMLPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "application/xhtml+xml;q=0.9");

        try {
            client.send();
        } catch (ResourceException e) {
            assertTrue(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void testErrorResponseContentTypeWithXMLPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "application/xml;q=0.9");

        try {
            client.send();
        } catch (ResourceException e) {
            assertFalse(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/plain;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void testErrorResponseContentTypeWithTextPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "text/plain");

        try {
            client.send();
        } catch (ResourceException e) {
            assertFalse(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/plain;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

    @Test
    void testErrorResponseContentTypeWithNoPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "*/*");

        try {
            client.send();
        } catch (ResourceException e) {
            assertFalse(e.getResponse().getBodyAsString().contains("html>"));
            assertTrue("text/plain;charset=UTF-8".equalsIgnoreCase(
                    e.getResponse().getHeaders().getFirstValue("Content-Type")));
        }
    }

}
