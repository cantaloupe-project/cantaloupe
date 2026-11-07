package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.ResourceException;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test of error responses.
 */
public class ErrorResourceTest extends ResourceTest {

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

    @Test
    void testIsClientDisconnectWithEofException() {
        assertTrue(ErrorResource.isClientDisconnect(new EofException("Closed")));
    }

    @Test
    void testIsClientDisconnectWithWrappedEofException() {
        Exception e = new RuntimeException(new IOException(new EofException()));
        assertTrue(ErrorResource.isClientDisconnect(e));
    }

    @Test
    void testIsClientDisconnectWithBrokenPipe() {
        assertTrue(ErrorResource.isClientDisconnect(
                new IOException("Broken pipe")));
    }

    @Test
    void testIsClientDisconnectWithConnectionReset() {
        assertTrue(ErrorResource.isClientDisconnect(
                new IOException("Connection reset by peer")));
    }

    @Test
    void testIsClientDisconnectWithOtherErrors() {
        assertFalse(ErrorResource.isClientDisconnect(null));
        assertFalse(ErrorResource.isClientDisconnect(new IOException()));
        assertFalse(ErrorResource.isClientDisconnect(
                new IOException("Disk full")));
        assertFalse(ErrorResource.isClientDisconnect(
                new IllegalArgumentException("Broken pipe")));
    }

    @Test
    void testDoGETDoesNotRenderPageWhenClientDisconnected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ErrorResource(new EofException("Closed"), request, response).doGET();

        assertEquals(0, response.getOutputStream().toByteArray().length);
    }

    @Test
    void testDoGETRendersPageForOtherErrors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ErrorResource(new IOException("Disk full"), request, response).doGET();

        assertEquals(500, response.getStatus());
        assertTrue(new String(response.getOutputStream().toByteArray(),
                java.nio.charset.StandardCharsets.UTF_8).contains("Disk full"));
    }

}
