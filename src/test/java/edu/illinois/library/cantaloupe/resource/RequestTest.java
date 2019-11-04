package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest extends BaseTest {

    private Request instance;

    @Test
    void testGetCookies() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.getHeaders().put("Cookie", List.of("fruit=apples; animal=cats"));
        instance = new Request(sr);

        Map<String, String> cookies = instance.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("apples", cookies.get("fruit"));
        assertEquals("cats", cookies.get("animal"));
    }

}
