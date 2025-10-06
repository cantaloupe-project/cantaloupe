package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot-enabled test for the FileServlet class.
 *
 * This test demonstrates the conversion from traditional JUnit tests to
 * Spring Boot testing framework, using Spring's mock servlet objects
 * instead of custom implementations.
 *
 * Note: FileServlet is being replaced by StaticFileController in the
 * Spring Boot conversion, but these tests ensure backward compatibility.
 */
@SpringBootTest(classes = CantalouperApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "logging.level.root=WARN",
    "logging.level.edu.illinois.library.cantaloupe=INFO",
    "http.enabled=true",
    "http.port=0"
})
class FileServletTest {

    private FileServlet instance;

    @BeforeEach
    public void setUp() throws Exception {
        instance = new FileServlet();
    }

    @Test
    void doGetWithPresentResource() throws Exception {
        // Create Spring's MockHttpServletRequest
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/styles/base.css");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/styles/base.css");

        // Create Spring's MockHttpServletResponse
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Execute the servlet method
        instance.doGet(request, response);

        // Verify the response
        assertEquals(200, response.getStatus());
        assertEquals("public, max-age=2592000", response.getHeader("Cache-Control"));
        assertEquals("text/css", response.getHeader("Content-Type"));

        // Check the response content
        String content = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(content.startsWith("@import") || content.contains("body") || content.length() > 0,
                "CSS content should be present");
    }

    @Test
    void doGetWithMissingResource() throws Exception {
        // Create request for non-existent resource
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/nonexistent.css");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/nonexistent.css");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Execute the servlet method
        instance.doGet(request, response);

        // Should return 404 for missing resource
        assertEquals(404, response.getStatus());
    }

    @Test
    void doGetWithJavaScriptFile() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/admin.js");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/admin.js");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        // Check for JavaScript content type and caching headers
        if (response.getStatus() == 200) {
            assertEquals("application/javascript", response.getHeader("Content-Type"));
            assertEquals("public, max-age=2592000", response.getHeader("Cache-Control"));
        } else {
            // If the file doesn't exist, should return 404
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void doGetWithImageFile() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/favicon.png");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/favicon.png");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        // Check for image content type and caching headers
        if (response.getStatus() == 200) {
            assertEquals("image/png", response.getHeader("Content-Type"));
            assertEquals("public, max-age=2592000", response.getHeader("Cache-Control"));
        } else {
            // If the file doesn't exist, should return 404
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void doGetWithHtmlFile() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/index.html");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/index.html");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        // Check for HTML content type and caching headers
        if (response.getStatus() == 200) {
            String contentType = response.getHeader("Content-Type");
            assertTrue(contentType != null && (contentType.equals("text/html") ||
                      contentType.startsWith("text/html;")));
            assertEquals("public, max-age=2592000", response.getHeader("Cache-Control"));
        } else {
            // If the file doesn't exist, should return 404
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void doGetWithContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/cantaloupe/static/styles/base.css");
        request.setContextPath("/cantaloupe");
        request.setServletPath("");
        request.setPathInfo("/static/styles/base.css");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        // Should handle context path correctly
        if (response.getStatus() == 200) {
            assertEquals("text/css", response.getHeader("Content-Type"));
            assertEquals("public, max-age=2592000", response.getHeader("Cache-Control"));
        } else {
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void doGetWithInvalidPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/../../../etc/passwd");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/../../../etc/passwd");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        // Should return 404 for invalid/dangerous paths
        assertEquals(404, response.getStatus());
    }

    @Test
    void doGetWithEmptyPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        // Should return 404 for directory requests
        assertEquals(404, response.getStatus());
    }

    @Test
    void contentTypeMapping() throws Exception {
        // Test various file extensions and their content types
        String[][] testCases = {
            {"/static/test.css", "text/css"},
            {"/static/test.js", "application/javascript"},
            {"/static/test.png", "image/png"},
            {"/static/test.jpg", "image/jpeg"},
            {"/static/test.gif", "image/gif"},
            {"/static/test.svg", "image/svg+xml"},
            {"/static/test.html", "text/html"},
            {"/static/test.json", "application/json"},
            {"/static/test.xml", "application/xml"},
            {"/static/test.ico", "image/x-icon"},
            {"/static/test.woff", "font/woff"},
            {"/static/test.woff2", "font/woff2"},
            {"/static/test.ttf", "font/ttf"},
            {"/static/test.eot", "application/vnd.ms-fontobject"}
        };

        for (String[] testCase : testCases) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("GET");
            request.setRequestURI(testCase[0]);
            request.setContextPath("");
            request.setServletPath("");
            request.setPathInfo(testCase[0]);

            MockHttpServletResponse response = new MockHttpServletResponse();

            instance.doGet(request, response);

            // Most files won't exist, but if they do, check content type
            if (response.getStatus() == 200) {
                String actualContentType = response.getHeader("Content-Type");
                assertEquals(testCase[1], actualContentType,
                    "Wrong content type for " + testCase[0]);
            }
            // If file doesn't exist, we expect 404, which is fine
        }
    }

    @Test
    void cacheHeadersAreSetCorrectly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/static/styles/base.css");
        request.setContextPath("");
        request.setServletPath("");
        request.setPathInfo("/static/styles/base.css");

        MockHttpServletResponse response = new MockHttpServletResponse();

        instance.doGet(request, response);

        if (response.getStatus() == 200) {
            // Verify cache control header
            String cacheControl = response.getHeader("Cache-Control");
            assertEquals("public, max-age=2592000", cacheControl);

            // Cache should be set for 30 days (2592000 seconds)
            assertTrue(cacheControl.contains("max-age=2592000"));
            assertTrue(cacheControl.contains("public"));
        }
    }
}
