package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContentTypeNegotiatorTest extends BaseTest {

    private ContentTypeNegotiator instance;
    private Headers headers;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        headers = new Headers();
        instance = new ContentTypeNegotiator(headers);
    }

    @Test
    void testGetPreferredMediaTypesWithAcceptHeaderSet() {
        headers.set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(3, types.size());
        assertEquals("application/xhtml+xml", types.get(0));
        assertEquals("text/html", types.get(1));
        assertEquals("text/plain", types.get(2));
    }

    @Test
    void testGetPreferredMediaTypesWithAcceptHeaderNotSet() {
        headers.removeAll("Accept");

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.isEmpty());
    }

    @Test
    void testGetPreferredMediaTypesIgnoresWildcardType() {
        headers.set("Accept", "text/html, */*");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(1, types.size());
        assertEquals("text/html", types.get(0));
    }

    @Test
    void testGetPreferredMediaTypesWithOnlyWildcardType() {
        headers.set("Accept", "*/*");

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.isEmpty());
    }

    @Test
    void testGetPreferredMediaTypesSortsCorrectly() {
        headers.set("Accept",
                "application/json;q=0.8, text/html;q=0.9, application/xml;q=1.0, text/plain;q=0.7");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(4, types.size());
        assertEquals("application/xml", types.get(0));  // q=1.0
        assertEquals("text/html", types.get(1));        // q=0.9
        assertEquals("application/json", types.get(2)); // q=0.8
        assertEquals("text/plain", types.get(3));       // q=0.7
    }

    @Test
    void testGetPreferredMediaTypesWithDefaultQualityValue() {
        headers.set("Accept", "application/json, text/html;q=0.9");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(2, types.size());
        assertEquals("application/json", types.get(0)); // q=1.0 (default)
        assertEquals("text/html", types.get(1));        // q=0.9
    }

    @Test
    void testNegotiateContentTypeWithMatchingType() {
        headers.set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> limitToTypes = Arrays.asList("application/json", "text/html", "application/xml");
        String result = instance.negotiateContentType(limitToTypes);
        assertEquals("text/html", result);
    }

    @Test
    void testNegotiateContentTypeWithBestMatchingType() {
        headers.set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> limitToTypes = Arrays.asList("application/xhtml+xml", "text/html", "application/xml");
        String result = instance.negotiateContentType(limitToTypes);
        assertEquals("application/xhtml+xml", result);
    }

    @Test
    void testNegotiateContentTypeWithNoMatchingType() {
        headers.set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> limitToTypes = Arrays.asList("application/json", "application/xml");
        String result = instance.negotiateContentType(limitToTypes);
        assertNull(result);
    }

    @Test
    void testNegotiateContentTypeWithNoAcceptHeader() {
        headers.removeAll("Accept");

        List<String> limitToTypes = Arrays.asList("application/json", "text/html");
        String result = instance.negotiateContentType(limitToTypes);
        assertNull(result);
    }

    @Test
    void testNegotiateContentTypeWithEmptyLimitToTypes() {
        headers.set("Accept", "text/html, application/json");

        List<String> limitToTypes = Arrays.asList();
        String result = instance.negotiateContentType(limitToTypes);
        assertNull(result);
    }
}
