package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaRequestContextTest extends BaseTest {

    private JavaRequestContext instance;

    private static RequestContext newRequestContext() {
        final RequestContext context = new RequestContext();
        // client IP
        context.setClientIP("1.2.3.4");
        // cookies
        Map<String,String> cookies = Map.of("cookie", "yes");
        context.setCookies(cookies);
        // metadata
        context.setMetadata(new Metadata());
        // operation list
        Identifier identifier = new Identifier("cats");
        Dimension fullSize    = new Dimension(200, 200);
        OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("gif")))
                .build();
        context.setOperationList(opList, fullSize);
        // page count
        context.setPageCount(3);
        // page number
        context.setPageNumber(3);
        // request headers
        Map<String,String> headers = Map.of("X-Cats", "Yes");
        context.setRequestHeaders(headers);
        // client-requested URI
        context.setRequestURI(new Reference("http://example.org/cats"));
        // local URI
        context.setLocalURI(new Reference("http://example.org/cats"));
        // scale constraint
        context.setScaleConstraint(new ScaleConstraint(1, 2));
        return context;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new JavaRequestContext(newRequestContext());
    }

    /* getClientIPAddress() */

    @Test
    void testGetClientIPAddressWhenSet() {
        assertEquals("1.2.3.4", instance.getClientIPAddress());
    }

    @Test
    void testGetClientIPAddressWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getClientIPAddress());
    }

    /* getCookies() */

    @Test
    void testGetCookiesWhenSet() {
        Map<String,String> actual = instance.getCookies();
        assertThrows(UnsupportedOperationException.class, actual::clear);
    }

    @Test
    void testGetCookiesWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        Map<String,String> actual = instance.getCookies();
        assertTrue(actual.isEmpty());
    }

    /* getFullSize() */

    @Test
    void testGetFullSizeWhenSet() {
        assertEquals(Map.of("width", 200, "height", 200),
                instance.getFullSize());
    }

    @Test
    void testGetFullSizeWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getFullSize());
    }

    /* getIdentifier() */

    @Test
    void testGetIdentifierWhenSet() {
        assertEquals("cats", instance.getIdentifier());
    }

    @Test
    void testGetIdentifierWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getIdentifier());
    }

    /* getLocalURI() */

    @Test
    void testGetLocalURIWhenSet() {
        assertEquals("http://example.org/cats", instance.getLocalURI());
    }

    @Test
    void testGetLocalURIWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getLocalURI());
    }

    /* getMetadata() */

    @Test
    void testGetMetadataWhenSet() {
        assertNotNull(instance.getMetadata());
    }

    @Test
    void testGetMetadataWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getMetadata());
    }

    /* getOperations() */

    @Test
    void testGetOperationsWhenSet() {
        assertNotNull(instance.getOperations());
    }

    @Test
    void testGetOperationsWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertTrue(instance.getOperations().isEmpty());
    }

    /* getOutputFormat() */

    @Test
    void testGetOutputFormatWhenSet() {
        assertEquals("image/gif", instance.getOutputFormat());
    }

    @Test
    void testGetOutputFormatWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getOutputFormat());
    }

    /* getPageCount() */

    @Test
    void testGetPageCountWhenSet() {
        assertEquals(3, instance.getPageCount());
    }

    @Test
    void testGetPageCountWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getPageCount());
    }

    /* getPageNumber() */

    @Test
    void testGetPageNumberWhenSet() {
        assertEquals(3, instance.getPageNumber());
    }

    @Test
    void testGetPageNumberWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getPageNumber());
    }

    /* getRequestHeaders() */

    @Test
    void testGetRequestHeadersWhenSet() {
        Map<String,String> actual = instance.getRequestHeaders();
        assertThrows(UnsupportedOperationException.class, actual::clear);
    }

    @Test
    void testGetRequestHeadersWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        Map<String,String> actual = instance.getRequestHeaders();
        assertTrue(actual.isEmpty());
    }

    /* getRequestURI() */

    @Test
    void testGetRequestURIWhenSet() {
        assertEquals("http://example.org/cats", instance.getRequestURI());
    }

    @Test
    void testGetRequestURIWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getRequestURI());
    }

    /* getResultingSize() */

    @Test
    void testGetResultingSizeWhenSet() {
        assertEquals(Map.of("width", 200, "height", 200),
                instance.getResultingSize());
    }

    @Test
    void testGetResultingSizeWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getResultingSize());
    }

    /* getScaleConstraint() */

    @Test
    void testGetScaleConstraintWhenSet() {
        assertArrayEquals(new int[] {1, 2}, instance.getScaleConstraint());
    }

    @Test
    void testGetScaleConstraintWhenNull() {
        instance = new JavaRequestContext(new RequestContext());
        assertNull(instance.getScaleConstraint());
    }

}