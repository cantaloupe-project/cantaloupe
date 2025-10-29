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
    void testForPathWithIIIFv1Routes() {
        Route route = Route.forPath(Route.IIIF_1_PATH + "/0bef-234a/info.json");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = Route.forPath(Route.IIIF_1_PATH + "/0bef-234a/0,0,100,100/max/0/native.jpg");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
        assertEquals("0,0,100,100", route.getPathArguments().get(1));
        assertEquals("max", route.getPathArguments().get(2));
        assertEquals("0", route.getPathArguments().get(3));
        assertEquals("native", route.getPathArguments().get(4));
        assertEquals("jpg", route.getPathArguments().get(5));

        route = Route.forPath(Route.IIIF_1_PATH + "/0bef-234a/0,0,100,100/max/0/native");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
        assertEquals("0,0,100,100", route.getPathArguments().get(1));
        assertEquals("max", route.getPathArguments().get(2));
        assertEquals("0", route.getPathArguments().get(3));
        assertEquals("native", route.getPathArguments().get(4));
    }

    @Test
    void testForPathWithInvalidRoute() {
        Route route = Route.forPath("/notfound");
        assertNull(route);
    }

}