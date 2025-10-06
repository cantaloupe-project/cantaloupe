package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Spring Boot base class for functional HTTP endpoint tests.
 *
 * This replaces the legacy ResourceTest class and provides modern Spring Boot
 * testing capabilities using TestRestTemplate for real HTTP requests to the
 * embedded server.
 */
@SpringBootTest(
    classes = CantalouperApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "logging.level.root=WARN",
    "logging.level.edu.illinois.library.cantaloupe=INFO",
    "http.enabled=true",
    "http.port=0",
    "https.enabled=false",
    "endpoint.iiif.1.enabled=true",
    "endpoint.iiif.2.enabled=true",
    "endpoint.iiif.3.enabled=true",
    "endpoint.admin.enabled=true",
    "endpoint.api.enabled=true",
    "endpoint.health.enabled=true",
    "processor.selection_strategy=ManualSelectionStrategy",
    "processor.ManualSelectionStrategy.pdf=PdfBoxProcessor",
    "processor.fallback=Java2dProcessor"
})
public abstract class SpringBootResourceTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int serverPort;

    /**
     * Subclasses must implement this to provide the endpoint path being tested.
     * For example: "/iiif/3" or "/health"
     */
    protected abstract String getEndpointPath();

    @BeforeEach
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();

        // Configure common test settings
        config.setProperty(Key.MAX_SCALE, 0);
        config.setProperty(Key.ADMIN_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY, "ManualSelectionStrategy");
        config.setProperty("processor.ManualSelectionStrategy.pdf", "PdfBoxProcessor");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Spring Boot handles server lifecycle automatically
        // Override in subclasses for custom cleanup
    }

    /**
     * Gets the base HTTP URI for the test server.
     */
    protected URI getBaseURI() {
        try {
            return new URI("http://localhost:" + serverPort);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to create base URI", e);
        }
    }

    /**
     * Gets the HTTP URI for a specific path relative to the endpoint.
     *
     * @param path Path relative to the endpoint path
     * @return Complete URI for the endpoint + path
     */
    protected URI getHTTPURI(String path) {
        try {
            String fullPath = getEndpointPath();
            if (path != null && !path.isEmpty()) {
                if (!path.startsWith("/")) {
                    fullPath += "/";
                }
                fullPath += path;
            }
            return new URI("http://localhost:" + serverPort + fullPath);
        } catch (URISyntaxException e) {
            fail("Failed to create URI for path: " + path + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the HTTPS URI for a specific path (if HTTPS is enabled).
     * Note: HTTPS is disabled in test configuration for simplicity.
     */
    protected URI getHTTPSURI(String path) {
        try {
            String fullPath = getEndpointPath();
            if (path != null && !path.isEmpty()) {
                if (!path.startsWith("/")) {
                    fullPath += "/";
                }
                fullPath += path;
            }
            return new URI("https://localhost:" + serverPort + fullPath);
        } catch (URISyntaxException e) {
            fail("Failed to create HTTPS URI for path: " + path + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Makes a GET request to the specified path.
     *
     * @param path Path relative to the endpoint
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> getForEntity(String path) {
        URI uri = getHTTPURI(path);
        return restTemplate.getForEntity(uri, String.class);
    }

    /**
     * Makes a GET request with custom headers.
     *
     * @param path Path relative to the endpoint
     * @param headers HTTP headers to include
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> getForEntityWithHeaders(String path, HttpHeaders headers) {
        URI uri = getHTTPURI(path);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
    }

    /**
     * Makes a POST request to the specified path.
     *
     * @param path Path relative to the endpoint
     * @param body Request body
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> postForEntity(String path, Object body) {
        URI uri = getHTTPURI(path);
        return restTemplate.postForEntity(uri, body, String.class);
    }

    /**
     * Makes a HEAD request to the specified path.
     *
     * @param path Path relative to the endpoint
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> headForEntity(String path) {
        URI uri = getHTTPURI(path);
        return restTemplate.exchange(uri, HttpMethod.HEAD, null, String.class);
    }

    /**
     * Makes an HTTP request with custom method and headers.
     *
     * @param path Path relative to the endpoint
     * @param method HTTP method
     * @param headers HTTP headers
     * @param body Request body (can be null)
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> exchange(String path, HttpMethod method,
                                            HttpHeaders headers, Object body) {
        URI uri = getHTTPURI(path);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(uri, method, entity, String.class);
    }

    /**
     * Makes a request with HTTP Basic Authentication.
     *
     * @param path Path relative to the endpoint
     * @param username Basic auth username
     * @param password Basic auth password
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> getWithBasicAuth(String path, String username, String password) {
        return restTemplate.withBasicAuth(username, password)
                          .getForEntity(getHTTPURI(path), String.class);
    }

    /**
     * Makes a request with custom TestRestTemplate configuration.
     * Useful for tests that need different timeout or error handling.
     *
     * @param path Path relative to the endpoint
     * @param customRestTemplate Pre-configured TestRestTemplate
     * @return ResponseEntity containing the response
     */
    protected ResponseEntity<String> getWithCustomTemplate(String path, TestRestTemplate customRestTemplate) {
        URI uri = getHTTPURI(path);
        return customRestTemplate.getForEntity(uri, String.class);
    }

    /**
     * Helper method to assert HTTP status codes.
     *
     * @param expectedStatus Expected HTTP status code
     * @param path Path to test
     */
    protected void assertStatus(int expectedStatus, String path) {
        ResponseEntity<String> response = getForEntity(path);
        org.junit.jupiter.api.Assertions.assertEquals(expectedStatus,
                response.getStatusCode().value(),
                "Expected status " + expectedStatus + " for path " + path +
                " but got " + response.getStatusCode().value());
    }

    /**
     * Helper method to assert that a response contains specific text.
     *
     * @param expectedContent Expected content substring
     * @param path Path to test
     */
    protected void assertResponseContains(String expectedContent, String path) {
        ResponseEntity<String> response = getForEntity(path);
        String body = response.getBody();
        org.junit.jupiter.api.Assertions.assertNotNull(body, "Response body should not be null");
        org.junit.jupiter.api.Assertions.assertTrue(body.contains(expectedContent),
                "Response should contain '" + expectedContent + "' but was: " + body);
    }

    /**
     * Helper method to assert response headers.
     *
     * @param headerName Expected header name
     * @param expectedValue Expected header value
     * @param path Path to test
     */
    protected void assertResponseHeader(String headerName, String expectedValue, String path) {
        ResponseEntity<String> response = getForEntity(path);
        String actualValue = response.getHeaders().getFirst(headerName);
        org.junit.jupiter.api.Assertions.assertEquals(expectedValue, actualValue,
                "Expected header " + headerName + " to be '" + expectedValue + "' but was '" + actualValue + "'");
    }
}
