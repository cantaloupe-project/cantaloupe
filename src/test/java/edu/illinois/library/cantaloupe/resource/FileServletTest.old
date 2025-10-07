package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FileServletTest extends BaseTest {

    private FileServlet instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new FileServlet();
    }

    @Test
    void doGetWithPresentResource() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURL("/static/styles/base.css");

        MockHttpServletResponse response = new MockHttpServletResponse();
        instance.doGet(request, response);

        // Check the status
        assertEquals(200, response.getStatus());
        // Check the headers
        assertEquals("public, max-age=2592000", response.getHeader("Cache-Control"));
        assertEquals("text/css", response.getHeader("Content-Type"));
        // Check the entity
        byte[] entityBytes = response.getOutputStream().toByteArray();
        String entity = new String(entityBytes, StandardCharsets.UTF_8);
        assertTrue(entity.startsWith("@import"));
    }

    @Test
    void doGetWithMissingResource() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURL("/static/bogus");

        MockHttpServletResponse response = new MockHttpServletResponse();
        instance.doGet(request, response);

        assertEquals(404, response.getStatus());
    }

}
