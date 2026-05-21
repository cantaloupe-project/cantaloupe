package edu.illinois.library.cantaloupe.resource;
import org.junit.jupiter.api.Test;

import edu.illinois.library.cantaloupe.http.Cookies;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class RequestContextDecoratorTest {
    @Test
    void testGetCookies() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.getHeaders().put("Cookie", List.of("fruit=apples; animal=cats",
                "shape=cube; car=ford"));
        Request instance = new Request(sr);

        Cookies cookies = RequestContextDecorator.getCookies(instance);
        assertEquals(4, cookies.size());
        assertEquals("apples", cookies.getFirstValue("fruit"));
        assertEquals("cats", cookies.getFirstValue("animal"));
        assertEquals("cube", cookies.getFirstValue("shape"));
        assertEquals("ford", cookies.getFirstValue("car"));
    }
}
