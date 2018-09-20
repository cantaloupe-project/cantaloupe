package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.resource.admin.AdminResource;
import edu.illinois.library.cantaloupe.resource.api.TaskResource;
import edu.illinois.library.cantaloupe.resource.api.TasksResource;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RouteTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testForPath() {
        Route route = Route.forPath("");
        assertEquals(LandingResource.class, route.getResource());

        route = Route.forPath("/");
        assertEquals(LandingResource.class, route.getResource());

        route = Route.forPath("/iiif/");
        assertEquals(TrailingSlashRemovingResource.class, route.getResource());

        route = Route.forPath(Route.ADMIN_CONFIG_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.admin.ConfigurationResource.class,
                route.getResource());

        route = Route.forPath(Route.ADMIN_PATH);
        assertEquals(AdminResource.class, route.getResource());

        route = Route.forPath(Route.ADMIN_STATUS_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.admin.StatusResource.class,
                route.getResource());

        route = Route.forPath(Route.CONFIGURATION_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.api.ConfigurationResource.class,
                route.getResource());

        route = Route.forPath(Route.HEALTH_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.api.HealthResource.class,
                route.getResource());

        route = Route.forPath(Route.STATUS_PATH);
        assertEquals(edu.illinois.library.cantaloupe.resource.api.StatusResource.class,
                route.getResource());

        route = Route.forPath(Route.TASKS_PATH);
        assertEquals(TasksResource.class, route.getResource());

        route = Route.forPath(Route.TASKS_PATH + "/0bef-234a");
        assertEquals(TaskResource.class, route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = Route.forPath(Route.IIIF_2_PATH + "/0bef-234a/info.json");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = Route.forPath(Route.IIIF_2_PATH + "/0bef-234a");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v2.IdentifierResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = Route.forPath(Route.IIIF_2_PATH + "/0bef-234a/0,0,100,100/max/0/default.jpg");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));
        assertEquals("0,0,100,100", route.getPathArguments().get(1));
        assertEquals("max", route.getPathArguments().get(2));
        assertEquals("0", route.getPathArguments().get(3));
        assertEquals("default", route.getPathArguments().get(4));
        assertEquals("jpg", route.getPathArguments().get(5));

        route = Route.forPath(Route.IIIF_1_PATH + "/0bef-234a/info.json");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class,
                route.getResource());
        assertEquals("0bef-234a", route.getPathArguments().get(0));

        route = Route.forPath(Route.IIIF_1_PATH + "/0bef-234a");
        assertEquals(edu.illinois.library.cantaloupe.resource.iiif.v1.IdentifierResource.class,
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

        route = Route.forPath("/notfound");
        assertNull(route);
    }

}