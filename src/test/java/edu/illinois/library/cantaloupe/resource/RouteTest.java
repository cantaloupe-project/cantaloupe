package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.resource.admin.AdminResource;
import edu.illinois.library.cantaloupe.resource.api.TaskResource;
import edu.illinois.library.cantaloupe.resource.api.TasksResource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteTest extends BaseTest {

    @Test
    void testForPathWithAdminRoutes() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.ADMIN_CONFIG_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.admin.ConfigurationResource.class,
                route.getResource());

        route = LegacyRoute.forPath(LegacyRoute.ADMIN_PATH);
        assertEquals(AdminResource.class, route.getResource());

        route = LegacyRoute.forPath(LegacyRoute.ADMIN_STATUS_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.admin.StatusResource.class,
                route.getResource());
    }

    @Test
    void testForPathWithConfigurationRoute() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.CONFIGURATION_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.api.ConfigurationResource.class,
                route.getResource());
    }

    @Test
    void testForPathWithStatusRoute() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.STATUS_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.api.StatusResource.class,
                route.getResource());
    }

    @Test
    void testForPathWithTasksRoutes() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.TASKS_PATH);
        assertEquals(TasksResource.class, route.getResource());

        route = LegacyRoute.forPath(LegacyRoute.TASKS_PATH + "/0bef-234a");
        assertEquals(TaskResource.class, route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
    }

    @Test
    void testForPathWithIIIFv3Routes() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.IIIF_3_PATH + "/0bef-234a/info.json");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v3.InformationResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_3_PATH + "/0bef-234a");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v3.IdentifierResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_3_PATH + "/0bef-234a/0,0,100,100/max/0/default.jpg");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v3.ImageResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
        assertEquals("0,0,100,100", route.getPathArguments().get(1));
        assertEquals("max", route.getPathArguments().get(2));
        assertEquals("0", route.getPathArguments().get(3));
        assertEquals("default", route.getPathArguments().get(4));
        assertEquals("jpg", route.getPathArguments().get(5));
    }

    @Test
    void testForPathWithIIIFv2Routes() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.IIIF_2_PATH + "/0bef-234a/info.json");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_2_PATH + "/0bef-234a");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v2.IdentifierResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_2_PATH + "/0bef-234a/0,0,100,100/max/0/default.jpg");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
        assertEquals("0,0,100,100", route.getPathArguments().get(1));
        assertEquals("max", route.getPathArguments().get(2));
        assertEquals("0", route.getPathArguments().get(3));
        assertEquals("default", route.getPathArguments().get(4));
        assertEquals("jpg", route.getPathArguments().get(5));
    }

    @Test
    void testForPathWithIIIFv1Routes() {
        LegacyRoute route = LegacyRoute.forPath(LegacyRoute.IIIF_1_PATH + "/0bef-234a/info.json");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_1_PATH + "/0bef-234a");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.IdentifierResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_1_PATH + "/0bef-234a/0,0,100,100/max/0/native.jpg");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
        assertEquals("0,0,100,100", route.getPathArguments().get(1));
        assertEquals("max", route.getPathArguments().get(2));
        assertEquals("0", route.getPathArguments().get(3));
        assertEquals("native", route.getPathArguments().get(4));
        assertEquals("jpg", route.getPathArguments().get(5));

        route = LegacyRoute.forPath(LegacyRoute.IIIF_1_PATH + "/0bef-234a/0,0,100,100/max/0/native");
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
        LegacyRoute route = LegacyRoute.forPath("/notfound");
        assertNull(route);
    }

}