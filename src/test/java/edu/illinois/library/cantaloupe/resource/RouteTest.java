package edu.illinois.library.cantaloupe.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.illinois.library.cantaloupe.test.BaseTest;

class RouteTest extends BaseTest {
    @Test
    void testForPathWithRootIIIFRoute() {
        Route route = Route.forPath("/iiif/");
        assertEquals(TrailingSlashRemovingResource.class, route.getResource());
    }

    @Test
    void testForPathWithInvalidRoute() {
        Route route = Route.forPath("/notfound");
        assertNull(route);
    }

}