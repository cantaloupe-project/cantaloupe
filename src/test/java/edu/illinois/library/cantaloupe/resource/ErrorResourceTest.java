package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.ResourceException;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional test of error responses.
 */
public class ErrorResourceTest extends ResourceTest {

    @After
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
    public void testErrorResponseContentTypeWithHTMLPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        try {
            client.send();
        } catch (ResourceException e) {
            assertTrue(e.getResponse().getContentAsString().contains("html>"));
            assertEquals("text/html;charset=UTF-8",
                    e.getResponse().getHeaders().get("Content-Type"));
        }
    }

    @Test
    public void testErrorResponseContentTypeWithXHTMLPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "application/xhtml+xml;q=0.9");

        try {
            client.send();
        } catch (ResourceException e) {
            assertTrue(e.getResponse().getContentAsString().contains("html>"));
            assertEquals("text/html;charset=UTF-8",
                    e.getResponse().getHeaders().get("Content-Type"));
        }
    }

    @Test
    public void testErrorResponseContentTypeWithXMLPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "application/xml;q=0.9");

        try {
            client.send();
        } catch (ResourceException e) {
            assertFalse(e.getResponse().getContentAsString().contains("html>"));
            assertEquals("text/plain;charset=UTF-8",
                    e.getResponse().getHeaders().get("Content-Type"));
        }
    }

    @Test
    public void testErrorResponseContentTypeWithTextPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "text/plain");

        try {
            client.send();
        } catch (ResourceException e) {
            assertFalse(e.getResponse().getContentAsString().contains("html>"));
            assertEquals("text/plain;charset=UTF-8",
                    e.getResponse().getHeaders().get("Content-Type"));
        }
    }

    @Test
    public void testErrorResponseContentTypeWithNoPreference()
            throws Exception {
        client = newClient("/bogus");
        client.getHeaders().add("Accept", "*/*");

        try {
            client.send();
        } catch (ResourceException e) {
            assertFalse(e.getResponse().getContentAsString().contains("html>"));
            assertEquals("text/plain;charset=UTF-8",
                    e.getResponse().getHeaders().get("Content-Type"));
        }
    }

}
