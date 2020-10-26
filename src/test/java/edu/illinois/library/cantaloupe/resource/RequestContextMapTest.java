package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static edu.illinois.library.cantaloupe.resource.RequestContextMap.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestContextMapTest extends BaseTest {

    private RequestContextMap<String,Object> instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

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
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withPageNumber(3)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(new Encode(Format.get("gif")))
                .build();
        context.setOperationList(opList, fullSize);
        // page count
        context.setPageCount(3);
        // request headers
        Map<String,String> headers = Map.of("X-Cats", "Yes");
        context.setRequestHeaders(headers);
        // client-requested URI
        context.setRequestURI(new URI("http://example.org/cats"));
        // local URI
        context.setLocalURI(new URI("http://example.org/cats"));

        instance = new RequestContextMap<>(context);
    }

    @Test
    void testClear() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.clear());
    }

    @Test
    void testContainsKey() {
        assertTrue(instance.containsKey(IDENTIFIER_KEY));
        assertFalse(instance.containsKey("bogus"));
    }

    @Test
    void testContainsValue() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.containsValue("bogus"));
    }

    @Test
    void testEntrySet() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.entrySet());
    }

    @Test
    void testGetWithNonNullValues() {
        // client IP
        assertTrue(instance.get(CLIENT_IP_KEY) instanceof String);
        // cookies
        assertTrue(instance.get(COOKIES_KEY) instanceof Map);
        assertThrows(UnsupportedOperationException.class,
                () -> ((Map<String,String>) instance.get(COOKIES_KEY)).clear());
        // full size
        assertTrue(instance.get(FULL_SIZE_KEY) instanceof Map);
        // identifier
        assertTrue(instance.get(IDENTIFIER_KEY) instanceof String);
        // local URI
        assertTrue(instance.get(LOCAL_URI_KEY) instanceof String);
        // metadata
        assertTrue(instance.get(METADATA_KEY) instanceof Map);
        // operations
        assertTrue(instance.get(OPERATIONS_KEY) instanceof List);
        // output format
        assertTrue(instance.get(OUTPUT_FORMAT_KEY) instanceof String);
        // page count
        assertTrue(instance.get(PAGE_COUNT_KEY) instanceof Integer);
        // page number
        assertTrue(instance.get(PAGE_NUMBER_KEY) instanceof Integer);
        // request headers
        assertTrue(instance.get(REQUEST_HEADERS_KEY) instanceof Map);
        assertThrows(UnsupportedOperationException.class,
                () -> ((Map<String,String>) instance.get(REQUEST_HEADERS_KEY)).clear());
        // request URI
        assertTrue(instance.get(REQUEST_URI_KEY) instanceof String);
        // resulting size
        assertTrue(instance.get(RESULTING_SIZE_KEY) instanceof Map);
        // scale constraint
        assertTrue(instance.get(SCALE_CONSTRAINT_KEY) instanceof List);
    }

    @Test
    void testGetWithNullValues() {
        instance = new RequestContextMap<>(new RequestContext());
        // client IP
        assertNull(instance.get(CLIENT_IP_KEY));
        // cookies
        assertNull(instance.get(COOKIES_KEY));
        // full size
        assertNull(instance.get(FULL_SIZE_KEY));
        // identifier
        assertNull(instance.get(IDENTIFIER_KEY));
        // local URI
        assertNull(instance.get(LOCAL_URI_KEY));
        // metadata
        assertNull(instance.get(METADATA_KEY));
        // operations
        assertNull(instance.get(OPERATIONS_KEY));
        // output format
        assertNull(instance.get(OUTPUT_FORMAT_KEY));
        // page count
        assertNull(instance.get(PAGE_COUNT_KEY));
        // page number
        assertNull(instance.get(PAGE_NUMBER_KEY));
        // request headers
        assertNull(instance.get(REQUEST_HEADERS_KEY));
        // request URI
        assertNull(instance.get(REQUEST_URI_KEY));
        // resulting size
        assertNull(instance.get(RESULTING_SIZE_KEY));
        // scale constraint
        assertNull(instance.get(SCALE_CONSTRAINT_KEY));
    }

    @Test
    void testIsEmpty() {
        assertFalse(instance.isEmpty());
        instance = new RequestContextMap<>(new RequestContext());
        assertTrue(instance.isEmpty());
    }

    @Test
    void testKeySet() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.keySet());
    }

    @Test
    void testPut() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.put("key", "value"));
    }

    @Test
    void testPutAll() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.putAll(Collections.emptyMap()));
    }

    @Test
    void testRemove() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.remove("key"));
    }

    @Test
    void testSize() {
        assertEquals(14, instance.size());
    }

    @Test
    void testValues() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.values());
    }

}