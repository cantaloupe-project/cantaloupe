package edu.illinois.library.cantaloupe.resource;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RequestContextTest {

    private RequestContext instance;

    @Before
    public void setUp() throws Exception {
        instance = new RequestContext();
        instance.setRequestURI("http://example.org/cats");
        instance.setClientIP("1.2.3.4");
        Map<String,String> headers = new HashMap<>();
        headers.put("X-Cats", "Yes");
        instance.setRequestHeaders(headers);
        Map<String,String> cookies = new HashMap<>();
        cookies.put("cookie", "yes");
        instance.setCookies(cookies);
    }

    @Test
    public void asMap() throws Exception {
        Map<String,Object> actual = instance.asMap();
        assertEquals("http://example.org/cats", actual.get("URI"));
        assertEquals("1.2.3.4", actual.get("clientIP"));
        assertEquals("Yes", ((Map) actual.get("headers")).get("X-Cats"));
        assertEquals("yes", ((Map) actual.get("cookies")).get("cookie"));
    }

}