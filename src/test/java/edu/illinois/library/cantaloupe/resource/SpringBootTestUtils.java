package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.resource.Request;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Spring Boot testing in Cantaloupe.
 *
 * This class provides helper methods for creating test requests,
 * configuring test environments, and asserting on Spring Boot
 * test results in the context of Cantaloupe's architecture.
 */
public class SpringBootTestUtils {

    /**
     * Creates a basic MockHttpServletRequest with common default values.
     */
    public static MockHttpServletRequest createBasicMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/");
        request.setServerName("localhost");
        request.setServerPort(8182);
        request.setScheme("http");
        return request;
    }

    /**
     * Creates a MockHttpServletRequest for testing IIIF endpoints.
     *
     * @param version IIIF API version (1, 2, or 3)
     * @param identifier Image identifier
     * @param region IIIF region parameter
     * @param size IIIF size parameter
     * @param rotation IIIF rotation parameter
     * @param quality IIIF quality parameter
     * @param format Image format
     */
    public static MockHttpServletRequest createIIIFRequest(int version,
                                                          String identifier,
                                                          String region,
                                                          String size,
                                                          String rotation,
                                                          String quality,
                                                          String format) {
        MockHttpServletRequest request = createBasicMockRequest();

        String path = String.format("/iiif/%d/%s/%s/%s/%s/%s.%s",
            version, identifier, region, size, rotation, quality, format);

        request.setRequestURI(path);
        request.addHeader("Accept", "image/*");

        return request;
    }

    /**
     * Creates a Request object from an MvcResult for testing.
     */
    public static Request createRequestFromMvcResult(MvcResult result) {
        return new Request(result.getRequest());
    }

    /**
     * Sets up a test configuration with common test values.
     */
    public static void setupTestConfiguration() {
        Configuration config = Configuration.getInstance();

        // Set common test configuration values
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");

        // Enable endpoints for testing
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);
        config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);
        config.setProperty(Key.API_ENABLED, true);
        config.setProperty(Key.ADMIN_ENABLED, true);
        config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, true);

        // Set HTTP configuration
        config.setProperty(Key.HTTP_ENABLED, true);
        config.setProperty(Key.HTTP_HOST, "localhost");
        config.setProperty(Key.HTTP_PORT, 8182);
        config.setProperty(Key.HTTPS_ENABLED, false);

        // Set logging level for tests
        config.setProperty(Key.APPLICATION_LOG_LEVEL, "WARN");
    }

    /**
     * Creates a MockHttpServletRequest with headers for testing.
     */
    public static MockHttpServletRequest createRequestWithHeaders(Map<String, String> headers) {
        MockHttpServletRequest request = createBasicMockRequest();

        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.addHeader(header.getKey(), header.getValue());
        }

        return request;
    }

    /**
     * Creates a MockHttpServletRequest with cookies for testing.
     */
    public static MockHttpServletRequest createRequestWithCookies(Map<String, String> cookies) {
        MockHttpServletRequest request = createBasicMockRequest();

        StringBuilder cookieHeader = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            if (!first) {
                cookieHeader.append("; ");
            }
            cookieHeader.append(cookie.getKey()).append("=").append(cookie.getValue());
            first = false;
        }

        if (cookieHeader.length() > 0) {
            request.addHeader("Cookie", cookieHeader.toString());
        }

        return request;
    }

    /**
     * Creates a MockHttpServletRequest with query parameters.
     */
    public static MockHttpServletRequest createRequestWithParams(String path,
                                                               Map<String, String> params) {
        MockHttpServletRequest request = createBasicMockRequest();
        request.setRequestURI(path);

        for (Map.Entry<String, String> param : params.entrySet()) {
            request.setParameter(param.getKey(), param.getValue());
        }

        return request;
    }

    /**
     * Validates that a Request object has expected values.
     */
    public static void assertRequestValid(Request request,
                                        String expectedMethod,
                                        String expectedPath) {
        if (request == null) {
            throw new AssertionError("Request is null");
        }

        if (!expectedMethod.equals(request.getMethod().toString())) {
            throw new AssertionError(String.format(
                "Expected method %s but got %s", expectedMethod, request.getMethod()));
        }

        String actualPath = request.getServletRequest().getRequestURI();
        if (!expectedPath.equals(actualPath)) {
            throw new AssertionError(String.format(
                "Expected path %s but got %s", expectedPath, actualPath));
        }
    }

    /**
     * Asserts that a Request has specific headers.
     */
    public static void assertRequestHasHeaders(Request request, Map<String, String> expectedHeaders) {
        Headers headers = request.getHeaders();

        for (Map.Entry<String, String> expected : expectedHeaders.entrySet()) {
            String actualValue = headers.getFirstValue(expected.getKey());
            if (!expected.getValue().equals(actualValue)) {
                throw new AssertionError(String.format(
                    "Expected header %s=%s but got %s=%s",
                    expected.getKey(), expected.getValue(),
                    expected.getKey(), actualValue));
            }
        }
    }

    /**
     * Asserts that a Request has specific cookies.
     */
    public static void assertRequestHasCookies(Request request, Map<String, String> expectedCookies) {
        Cookies cookies = request.getCookies();

        for (Map.Entry<String, String> expected : expectedCookies.entrySet()) {
            String actualValue = cookies.getFirstValue(expected.getKey());
            if (!expected.getValue().equals(actualValue)) {
                throw new AssertionError(String.format(
                    "Expected cookie %s=%s but got %s=%s",
                    expected.getKey(), expected.getValue(),
                    expected.getKey(), actualValue));
            }
        }
    }

    /**
     * Creates a test configuration map with common Spring Boot test properties.
     */
    public static Map<String, Object> getTestProperties() {
        Map<String, Object> props = new HashMap<>();

        // Spring Boot properties
        props.put("spring.main.banner-mode", "off");
        props.put("logging.level.root", "WARN");
        props.put("logging.level.edu.illinois.library.cantaloupe", "INFO");
        props.put("server.port", "0"); // Random port for testing

        // Cantaloupe properties
        props.put("http.enabled", "true");
        props.put("http.port", "0");
        props.put("https.enabled", "false");
        props.put("endpoint.iiif.1.enabled", "true");
        props.put("endpoint.iiif.2.enabled", "true");
        props.put("endpoint.iiif.3.enabled", "true");
        props.put("endpoint.api.enabled", "true");
        props.put("endpoint.admin.enabled", "true");
        props.put("endpoint.health.enabled", "true");

        return props;
    }

    /**
     * Converts test properties map to Spring Boot property array format.
     */
    public static String[] getTestPropertiesAsArray() {
        Map<String, Object> props = getTestProperties();
        return props.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toArray(String[]::new);
    }

    /**
     * Creates a mock HTTPS request for SSL testing.
     */
    public static MockHttpServletRequest createHttpsRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI(path);
        request.setServerName("secure.example.com");
        request.setServerPort(443);
        request.setScheme("https");
        request.setSecure(true);

        return request;
    }

    /**
     * Creates a mock request with specific remote address for testing
     * IP-based functionality.
     */
    public static MockHttpServletRequest createRequestFromRemoteAddr(String remoteAddr) {
        MockHttpServletRequest request = createBasicMockRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    /**
     * Helper method to print request details for debugging tests.
     */
    public static void printRequestDetails(HttpServletRequest request) {
        System.out.println("=== Request Details ===");
        System.out.println("Method: " + request.getMethod());
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("Query String: " + request.getQueryString());
        System.out.println("Remote Addr: " + request.getRemoteAddr());
        System.out.println("Scheme: " + request.getScheme());
        System.out.println("Server: " + request.getServerName() + ":" + request.getServerPort());

        System.out.println("Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            System.out.println("  " + name + ": " + request.getHeader(name));
        }

        System.out.println("Parameters:");
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> param : params.entrySet()) {
            System.out.println("  " + param.getKey() + ": " + String.join(", ", param.getValue()));
        }

        System.out.println("======================");
    }

    /**
     * Cleans up test configuration after tests.
     */
    public static void cleanupTestConfiguration() {
        ConfigurationFactory.clearInstance();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        System.clearProperty("cantaloupe.test");
    }
}
