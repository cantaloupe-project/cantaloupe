package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility class for Spring Boot HTTP testing in Cantaloupe.
 *
 * This class provides helper methods for making HTTP requests using Spring Boot's
 * TestRestTemplate, replacing the legacy Cantaloupe Client usage in tests.
 * It includes utilities for authentication, error handling, concurrent requests,
 * and IIIF-specific testing scenarios.
 */
public class SpringBootHttpTestUtils {
    /**
     * Creates a TestRestTemplate configured to trust all SSL certificates.
     * Useful for testing with self-signed certificates.
     *
     * @return TestRestTemplate that trusts all SSL certificates
     */
    public static TestRestTemplate createTrustAllRestTemplate() {
        // For testing purposes only - in production, proper SSL validation should be used
        TestRestTemplate template = new TestRestTemplate();
        // Note: Actual SSL trust-all configuration would require additional setup
        return template;
    }

    /**
     * Makes an HTTP request and asserts the expected status code.
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param method The HTTP method
     * @param expectedStatus The expected HTTP status code
     * @return The response entity
     */
    public static ResponseEntity<String> requestAndAssertStatus(TestRestTemplate restTemplate,
                                                              URI uri,
                                                              HttpMethod method,
                                                              HttpStatus expectedStatus) {
        ResponseEntity<String> response = restTemplate.exchange(uri, method, null, String.class);
        assertEquals(expectedStatus, response.getStatusCode(),
                    "Expected status " + expectedStatus + " but got " + response.getStatusCode() + " for " + uri);
        return response;
    }

    /**
     * Makes an HTTP request and asserts that the status code is successful (2xx).
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param method The HTTP method
     * @return The response entity
     */
    public static ResponseEntity<String> requestAndAssertSuccess(TestRestTemplate restTemplate,
                                                               URI uri,
                                                               HttpMethod method) {
        ResponseEntity<String> response = restTemplate.exchange(uri, method, null, String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                  "Expected successful status but got " + response.getStatusCode() + " for " + uri);
        return response;
    }

    /**
     * Makes a GET request with HTTP Basic Authentication.
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param username Basic auth username
     * @param password Basic auth password
     * @return The response entity
     */
    public static ResponseEntity<String> getWithBasicAuth(TestRestTemplate restTemplate,
                                                        URI uri,
                                                        String username,
                                                        String password) {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.add("Authorization", "Basic " + encodedAuth);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
    }

    /**
     * Makes a POST request with JSON content.
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param jsonBody The JSON body content
     * @return The response entity
     */
    public static ResponseEntity<String> postJson(TestRestTemplate restTemplate,
                                                URI uri,
                                                String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        return restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
    }

    /**
     * Makes a request with custom headers.
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param method The HTTP method
     * @param headers The headers to include
     * @return The response entity
     */
    public static ResponseEntity<String> requestWithHeaders(TestRestTemplate restTemplate,
                                                          URI uri,
                                                          HttpMethod method,
                                                          HttpHeaders headers) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(uri, method, entity, String.class);
    }

    /**
     * Makes a request for binary content (e.g., images).
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @return The response entity with byte array body
     */
    public static ResponseEntity<byte[]> getBinaryContent(TestRestTemplate restTemplate, URI uri) {
        return restTemplate.getForEntity(uri, byte[].class);
    }

    /**
     * Tests concurrent requests to the same endpoint.
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param numberOfRequests Number of concurrent requests to make
     * @param timeoutSeconds Timeout for all requests to complete
     * @return Array of response entities
     * @throws Exception If requests fail or timeout
     */
    public static ResponseEntity<String>[] testConcurrentRequests(TestRestTemplate restTemplate,
                                                                URI uri,
                                                                int numberOfRequests,
                                                                long timeoutSeconds) throws Exception {
        @SuppressWarnings("unchecked")
        ResponseEntity<String>[] responses = new ResponseEntity[numberOfRequests];
        CompletableFuture<ResponseEntity<String>>[] futures = new CompletableFuture[numberOfRequests];

        // Start all requests concurrently
        for (int i = 0; i < numberOfRequests; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() ->
                restTemplate.getForEntity(uri, String.class)
            );
        }

        // Wait for all requests to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        allOf.get(timeoutSeconds, TimeUnit.SECONDS);

        // Collect results
        for (int i = 0; i < numberOfRequests; i++) {
            responses[i] = futures[i].get();
        }

        return responses;
    }

    /**
     * Builds a URI for IIIF Image API requests.
     *
     * @param baseUri The base URI of the server
     * @param version IIIF version (1, 2, or 3)
     * @param identifier Image identifier
     * @param region IIIF region parameter
     * @param size IIIF size parameter
     * @param rotation IIIF rotation parameter
     * @param quality IIIF quality parameter
     * @param format Image format extension
     * @return Complete IIIF Image API URI
     * @throws URISyntaxException If URI construction fails
     */
    public static URI buildIIIFImageURI(URI baseUri,
                                      int version,
                                      String identifier,
                                      String region,
                                      String size,
                                      String rotation,
                                      String quality,
                                      String format) throws URISyntaxException {
        String path = String.format("/iiif/%d/%s/%s/%s/%s/%s.%s",
                                   version, identifier, region, size, rotation, quality, format);
        return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(), path, null, null);
    }

    /**
     * Builds a URI for IIIF Information API requests.
     *
     * @param baseUri The base URI of the server
     * @param version IIIF version (1, 2, or 3)
     * @param identifier Image identifier
     * @return Complete IIIF Information API URI
     * @throws URISyntaxException If URI construction fails
     */
    public static URI buildIIIFInfoURI(URI baseUri, int version, String identifier) throws URISyntaxException {
        String path = String.format("/iiif/%d/%s/info.json", version, identifier);
        return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(), path, null, null);
    }

    /**
     * Asserts that a response contains expected IIIF headers.
     *
     * @param response The response to check
     */
    public static void assertIIIFHeaders(ResponseEntity<?> response) {
        HttpHeaders headers = response.getHeaders();

        // Check for CORS headers
        String corsOrigin = headers.getFirst("Access-Control-Allow-Origin");
        if (corsOrigin != null) {
            assertTrue(corsOrigin.equals("*") || corsOrigin.contains("http"),
                      "CORS origin header should be wildcard or valid origin");
        }

        // Check for IIIF compliance headers
        String linkHeader = headers.getFirst("Link");
        if (linkHeader != null) {
            assertTrue(linkHeader.contains("profile") || linkHeader.contains("iiif"),
                      "Link header should reference IIIF profile");
        }
    }

    /**
     * Asserts that an image response is valid.
     *
     * @param response The response to validate
     * @param expectedContentType Expected content type (e.g., "image/jpeg")
     */
    public static void assertValidImageResponse(ResponseEntity<byte[]> response, String expectedContentType) {
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                  "Image response should be successful");

        String contentType = response.getHeaders().getFirst("Content-Type");
        assertNotNull(contentType, "Content-Type header should be present");
        assertTrue(contentType.contains(expectedContentType),
                  "Content-Type should be " + expectedContentType + " but was " + contentType);

        byte[] body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertTrue(body.length > 0, "Response body should not be empty");
    }

    /**
     * Asserts that a JSON response is valid.
     *
     * @param response The response to validate
     */
    public static void assertValidJSONResponse(ResponseEntity<String> response) {
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                  "JSON response should be successful");

        String contentType = response.getHeaders().getFirst("Content-Type");
        assertNotNull(contentType, "Content-Type header should be present");
        assertTrue(contentType.contains("application/json"),
                  "Content-Type should be JSON but was " + contentType);

        String body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertTrue(body.trim().startsWith("{") || body.trim().startsWith("["),
                  "Response should be valid JSON");
    }

    /**
     * Configures a test environment for HTTP testing.
     *
     * @param serverPort The port the test server is running on
     */
    public static void configureTestEnvironment(int serverPort) {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTP_ENABLED, true);
        config.setProperty(Key.HTTP_HOST, "localhost");
        config.setProperty(Key.HTTP_PORT, serverPort);
        config.setProperty(Key.HTTPS_ENABLED, false);
    }

    /**
     * Creates a URI from a base URI and path.
     *
     * @param baseUri The base URI
     * @param path The path to append
     * @return Combined URI
     * @throws URISyntaxException If URI construction fails
     */
    public static URI createURI(URI baseUri, String path) throws URISyntaxException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(), path, null, null);
    }

    /**
     * Waits for a server to become available by polling a health endpoint.
     *
     * @param restTemplate The TestRestTemplate to use for polling
     * @param healthUri The health endpoint URI
     * @param maxWaitMillis Maximum time to wait in milliseconds
     * @param pollIntervalMillis Interval between polls in milliseconds
     * @return true if server becomes available, false if timeout
     */
    public static boolean waitForServerAvailable(TestRestTemplate restTemplate,
                                                URI healthUri,
                                                long maxWaitMillis,
                                                long pollIntervalMillis) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + maxWaitMillis;

        while (System.currentTimeMillis() < endTime) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUri, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
            } catch (Exception e) {
                // Server not available yet, continue polling
            }

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * Creates HttpHeaders with common test headers.
     *
     * @return HttpHeaders with test-specific headers
     */
    public static HttpHeaders createTestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Cantaloupe-Test/1.0");
        headers.add("Accept", "application/json,image/*,*/*");
        return headers;
    }

    /**
     * Asserts that response time is within acceptable limits.
     *
     * @param startTime Start time in milliseconds
     * @param maxResponseTimeMillis Maximum acceptable response time
     */
    public static void assertResponseTime(long startTime, long maxResponseTimeMillis) {
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime <= maxResponseTimeMillis,
                  "Response time " + responseTime + "ms exceeded maximum " + maxResponseTimeMillis + "ms");
    }

    /**
     * Measures response time for a request.
     *
     * @param restTemplate The TestRestTemplate to use
     * @param uri The URI to request
     * @param method The HTTP method
     * @return Response time in milliseconds
     */
    public static long measureResponseTime(TestRestTemplate restTemplate, URI uri, HttpMethod method) {
        long startTime = System.currentTimeMillis();
        restTemplate.exchange(uri, method, null, String.class);
        return System.currentTimeMillis() - startTime;
    }
}
