package edu.illinois.library.cantaloupe.resource;
import edu.illinois.library.cantaloupe.http.Cookies;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestContextDecoratorTest {
    @Test
    void testGetCookies() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.getHeaders().put("Cookie", List.of("fruit=apples; animal=cats",
                "shape=cube; car=ford"));
        Request instance = new Request(sr, Collections.emptyList());

        Cookies cookies = RequestContextDecorator.getCookies(instance);
        assertEquals(4, cookies.size());
        assertEquals("apples", cookies.getFirstValue("fruit"));
        assertEquals("cats", cookies.getFirstValue("animal"));
        assertEquals("cube", cookies.getFirstValue("shape"));
        assertEquals("ford", cookies.getFirstValue("car"));
    }
}
