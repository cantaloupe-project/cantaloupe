package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static edu.illinois.library.cantaloupe.resource.RequestContext.CLIENT_IP_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.COOKIES_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.FULL_SIZE_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.IDENTIFIER_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.LOCAL_URI_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.METADATA_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.OPERATIONS_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.OUTPUT_FORMAT_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.REQUEST_HEADERS_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.REQUEST_URI_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.RESULTING_SIZE_KEY;
import static edu.illinois.library.cantaloupe.resource.RequestContext.SCALE_CONSTRAINT_KEY;
import static org.junit.jupiter.api.Assertions.*;

public class RequestContextTest extends BaseTest {

    private RequestContext instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new RequestContext();

        // client IP
        instance.setClientIP("1.2.3.4");

        // cookies
        Map<String,String> cookies = new HashMap<>();
        cookies.put("cookie", "yes");
        instance.setCookies(cookies);

        // operation list
        Identifier identifier = new Identifier("cats");
        Dimension fullSize = new Dimension(200, 200);
        OperationList opList = new OperationList(
                identifier, new Encode(Format.get("gif")));
        instance.setOperationList(opList, fullSize);

        // request headers
        Map<String,String> headers = new HashMap<>();
        headers.put("X-Cats", "Yes");
        instance.setRequestHeaders(headers);

        // client-requested URI
        instance.setRequestURI(new URI("http://example.org/cats"));

        // local URI
        instance.setLocalURI(new URI("http://example.org/cats"));
    }

    @Test
    void testSetClientIP() {
        instance.setClientIP("3.4.5.6");
        assertEquals("3.4.5.6", instance.toMap().get(CLIENT_IP_KEY));
        instance.setClientIP(null);
        assertNull(instance.toMap().get(CLIENT_IP_KEY));
    }

    @Test
    void testSetCookies() {
        instance.setCookies(Collections.emptyMap());
        assertNotNull(instance.toMap().get(COOKIES_KEY));
        instance.setCookies(null);
        assertNull(instance.toMap().get(COOKIES_KEY));
    }

    @Test
    void testSetIdentifier() {
        instance.setIdentifier(new Identifier("cats"));
        assertEquals("cats", instance.toMap().get(IDENTIFIER_KEY));
        instance.setIdentifier(null);
        assertNull(instance.toMap().get(IDENTIFIER_KEY));
    }

    @Test
    void testSetLocalURI() throws Exception {
        instance.setLocalURI(new URI("http://example.org/"));
        assertNotNull(instance.toMap().get(LOCAL_URI_KEY));
        instance.setLocalURI(null);
        assertNull(instance.toMap().get(LOCAL_URI_KEY));
    }

    @Test
    void testSetMetadata() {
        instance.setMetadata(new Metadata());
        assertNotNull(instance.toMap().get(METADATA_KEY));
        instance.setMetadata(null);
        assertNull(instance.toMap().get(METADATA_KEY));
    }

    @Test
    void testSetOperationList() {
        OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.get("jpg")));
        instance.setOperationList(opList, new Dimension(5, 5));
        assertNotNull(instance.toMap().get(FULL_SIZE_KEY));
        assertNotNull(instance.toMap().get(IDENTIFIER_KEY));
        assertNotNull(instance.toMap().get(OPERATIONS_KEY));
        assertNotNull(instance.toMap().get(OUTPUT_FORMAT_KEY));
        assertNotNull(instance.toMap().get(RESULTING_SIZE_KEY));

        instance.setOperationList(null, null);
        assertNull(instance.toMap().get(FULL_SIZE_KEY));
        assertNull(instance.toMap().get(IDENTIFIER_KEY));
        assertNull(instance.toMap().get(OPERATIONS_KEY));
        assertNull(instance.toMap().get(OUTPUT_FORMAT_KEY));
        assertNull(instance.toMap().get(RESULTING_SIZE_KEY));
    }

    @Test
    void testSetRequestHeaders() {
        instance.setRequestHeaders(Collections.emptyMap());
        assertNotNull(instance.toMap().get(REQUEST_HEADERS_KEY));
        instance.setRequestHeaders(null);
        assertNull(instance.toMap().get(REQUEST_HEADERS_KEY));
    }

    @Test
    void testSetRequestURI() throws Exception {
        instance.setRequestURI(new URI("http://example.org/"));
        assertNotNull(instance.toMap().get(REQUEST_URI_KEY));
        instance.setRequestURI(null);
        assertNull(instance.toMap().get(REQUEST_URI_KEY));
    }

    @Test
    void testSetScaleConstraint() {
        instance.setScaleConstraint(new ScaleConstraint(1, 2));
        assertNotNull(instance.toMap().get(SCALE_CONSTRAINT_KEY));
        instance.setScaleConstraint(null);
        assertNull(instance.toMap().get(SCALE_CONSTRAINT_KEY));
    }

    @Test
    void testToMap() {
        Map<String,Object> actual = instance.toMap();
        // client IP
        assertEquals("1.2.3.4", actual.get(CLIENT_IP_KEY));
        // cookies
        assertEquals("yes", ((Map<?, ?>) actual.get(COOKIES_KEY)).get("cookie"));
        // full size
        assertNotNull(actual.get(FULL_SIZE_KEY));
        // identifier
        assertEquals("cats", actual.get(IDENTIFIER_KEY));
        // operations
        assertNotNull(actual.get(OPERATIONS_KEY));
        // output format
        assertEquals("image/gif", actual.get(OUTPUT_FORMAT_KEY));
        // request headers
        assertEquals("Yes", ((Map<?, ?>) actual.get(REQUEST_HEADERS_KEY)).get("X-Cats"));
        // request URI
        assertEquals("http://example.org/cats",
                actual.get(REQUEST_URI_KEY));
        // resulting size
        assertNotNull(actual.get(RESULTING_SIZE_KEY));
    }

    @Test
    void testToMapLiveView() {
        instance.setClientIP("2.3.4.5");
        Map<String,Object> actual = instance.toMap();
        assertEquals("2.3.4.5", actual.get(CLIENT_IP_KEY));
        instance.setClientIP("3.4.5.6");
        assertEquals("3.4.5.6", actual.get(CLIENT_IP_KEY));
    }

}
