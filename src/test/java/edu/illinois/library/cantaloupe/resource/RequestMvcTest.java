package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.test.SpringBootBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc-based test for Request class using Spring Boot testing framework.
 * This provides detailed testing of Request objects within the Spring MVC context
 * using MockMvc for more precise control over HTTP requests.
 */
@SpringBootTest(classes = CantalouperApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "http.enabled=true",
    "http.port=0",
    "https.enabled=false",
    "endpoint.iiif.1.enabled=true",
    "endpoint.iiif.2.enabled=true",
    "endpoint.iiif.3.enabled=true",
    "spring.main.banner-mode=off"
})
class RequestMvcTest extends SpringBootBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Request testRequest;

    @BeforeEach
    void setUpRequest() {
        // Reset the test request for each test
        testRequest = null;
    }

    @Test
    void testRequestWithMockMvcGet() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals(Method.GET, request.getMethod());
        assertEquals("/", servletRequest.getRequestURI());
        assertNotNull(request.getHeaders());
    }

    @Test
    void testRequestWithHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/")
                .header("User-Agent", "Test-Agent/1.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("X-Custom-Header", "test-value"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        Headers headers = request.getHeaders();
        assertEquals("Test-Agent/1.0", headers.getFirstValue("User-Agent"));
        assertEquals("text/html,application/xhtml+xml", headers.getFirstValue("Accept"));
        assertEquals("en-US,en;q=0.9", headers.getFirstValue("Accept-Language"));
        assertEquals("test-value", headers.getFirstValue("X-Custom-Header"));
    }

    @Test
    void testRequestWithCookies() throws Exception {
        MvcResult result = mockMvc.perform(get("/")
                .cookie(new jakarta.servlet.http.Cookie("sessionId", "abc123"))
                .cookie(new jakarta.servlet.http.Cookie("theme", "dark"))
                .header("Cookie", "fruit=apple; color=blue"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        Cookies cookies = request.getCookies();
        // Note: MockMvc cookies and header cookies are handled differently
        assertTrue(cookies.size() >= 2);
    }

    @Test
    void testRequestWithQueryParameters() throws Exception {
        MvcResult result = mockMvc.perform(get("/")
                .param("param1", "value1")
                .param("param2", "value2")
                .param("search", "test query"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals("value1", servletRequest.getParameter("param1"));
        assertEquals("value2", servletRequest.getParameter("param2"));
        assertEquals("test query", servletRequest.getParameter("search"));

        Reference reference = request.getReference();
        assertTrue(reference.toString().contains("param1=value1"));
        assertTrue(reference.toString().contains("param2=value2"));
    }

    @Test
    void testRequestWithDifferentHttpMethods() throws Exception {
        // Test GET
        MvcResult getResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        Request getRequest = new Request(getResult.getRequest());
        assertEquals(Method.GET, getRequest.getMethod());

        // Test POST
        MvcResult postResult = mockMvc.perform(post("/")
                .content("{\"test\": \"data\"}")
                .contentType("application/json"))
                .andReturn(); // May not return 200, but shouldn't crash

        Request postRequest = new Request(postResult.getRequest());
        assertEquals(Method.POST, postRequest.getMethod());

        // Test HEAD
        MvcResult headResult = mockMvc.perform(head("/"))
                .andExpect(status().isOk())
                .andReturn();

        Request headRequest = new Request(headResult.getRequest());
        assertEquals(Method.HEAD, headRequest.getMethod());

        // Test OPTIONS
        MvcResult optionsResult = mockMvc.perform(options("/"))
                .andReturn();

        Request optionsRequest = new Request(optionsResult.getRequest());
        assertEquals(Method.OPTIONS, optionsRequest.getMethod());
    }

    @Test
    void testRequestToIIIFEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals("/iiif/3", servletRequest.getRequestURI());
        assertEquals(Method.GET, request.getMethod());

        Reference reference = request.getReference();
        assertTrue(reference.toString().contains("/iiif/3"));
    }

    @Test
    void testRequestWithComplexPath() throws Exception {
        String complexPath = "/iiif/2/test%20image/full/max/0/default.jpg";

        MvcResult result = mockMvc.perform(get(complexPath))
                .andReturn(); // May return error status, but request should be created properly

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals(complexPath, servletRequest.getRequestURI());
        assertNotNull(request.getReference());
    }

    @Test
    void testRequestWithMultipleHeaderValues() throws Exception {
        MvcResult result = mockMvc.perform(get("/")
                .header("Accept", "text/html")
                .header("Accept", "application/xml")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Encoding", "deflate"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        Headers headers = request.getHeaders();
        assertTrue(headers.getAll("Accept").size() >= 1);
        assertTrue(headers.getAll("Accept-Encoding").size() >= 1);
    }

    @Test
    void testRequestContextPath() throws Exception {
        MvcResult result = mockMvc.perform(get("/")
                .contextPath("/cantaloupe"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals("/cantaloupe", request.getContextPath());
    }

    @Test
    void testRequestRemoteAddress() throws Exception {
        MvcResult result = mockMvc.perform(get("/")
                .header("X-Forwarded-For", "192.168.1.100")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals("10.0.0.1", request.getRemoteAddr());
    }

    @Test
    void testRequestReference() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/path")
                .param("query", "value")
                .header("Host", "example.com:8080")
                .secure(false))
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        Reference reference = request.getReference();
        assertNotNull(reference);
        assertTrue(reference.toString().contains("/test/path"));
    }

    @Test
    void testRequestWithSecureConnection() throws Exception {
        MvcResult result = mockMvc.perform(get("/secure")
                .secure(true)
                .header("Host", "secure.example.com"))
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertTrue(servletRequest.isSecure());
        assertEquals("https", servletRequest.getScheme());

        Reference reference = request.getReference();
        assertTrue(reference.toString().startsWith("https://"));
    }

    @Test
    void testHealthEndpointRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals("/health", servletRequest.getRequestURI());
        assertEquals(Method.GET, request.getMethod());
    }

    @Test
    void testRequestWithLargeHeaders() throws Exception {
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeValue.append("a");
        }

        MvcResult result = mockMvc.perform(get("/")
                .header("X-Large-Header", largeValue.toString())
                .header("User-Agent", "TestAgent"))
                .andExpect(status().isOk())
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        Headers headers = request.getHeaders();
        assertEquals(largeValue.toString(), headers.getFirstValue("X-Large-Header"));
        assertEquals("TestAgent", headers.getFirstValue("User-Agent"));
    }

    @Test
    void testRequestEncoding() throws Exception {
        String unicodePath = "/test/üñíçødé";

        MvcResult result = mockMvc.perform(get(unicodePath)
                .characterEncoding("UTF-8"))
                .andReturn();

        HttpServletRequest servletRequest = result.getRequest();
        Request request = new Request(servletRequest);

        assertEquals(unicodePath, servletRequest.getRequestURI());
        assertNotNull(request.getReference());
    }
}
