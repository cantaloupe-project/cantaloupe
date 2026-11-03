package edu.illinois.library.cantaloupe.resource;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.http.Cookies;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestContextDecoratorTest {
    @Test
    void testGetCookies() {
        // Set up configuration system properties
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.getHeaders().put("Cookie", List.of("fruit=apples; animal=cats",
                "shape=cube; car=ford"));
        IIIFRequest instance = new IIIFRequest(sr, Collections.emptyList(), Configuration.getInstance());

        Cookies cookies = RequestContextDecorator.getCookies(instance);
        assertEquals(4, cookies.size());
        assertEquals("apples", cookies.getFirstValue("fruit"));
        assertEquals("cats", cookies.getFirstValue("animal"));
        assertEquals("cube", cookies.getFirstValue("shape"));
        assertEquals("ford", cookies.getFirstValue("car"));
    }
}
