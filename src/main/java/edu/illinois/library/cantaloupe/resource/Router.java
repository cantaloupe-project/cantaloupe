package edu.illinois.library.cantaloupe.resource;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;    

/**
 * Associates a URI path pattern with an {@link Controller}
 * implementation.
 *
 */
public final class Router {

    static class Route {
        public final Class<? extends Controller> controller;
        public final String identifier;

        public Route(Class<? extends Controller> controller, String identifier) {
            this.controller = controller;
            this.identifier = identifier;
        }

        public Controller getInstance(HttpServletRequest request, HttpServletResponse response) throws Exception {
            return controller.getDeclaredConstructor(HttpServletRequest.class, HttpServletResponse.class).newInstance(request,response);
        }
    }

    /**
     * @param path URI path relative to the context path.
     * @return     Route corresponding to the given path, or {@code null} if
     *             there is no match.
     */
    static boolean execute(String method, String path, HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        Route route = findControllerForRoute(path);
        if (route == null) {
            return false;
        }
        try {
        Controller controller = route.getInstance(request, response);

        if (method.equals("OPTIONS")) {
            controller.doOptions();
        } else {
            controller.doGet(new Request(request));
        }
    } catch (Exception e) {
        System.out.println("Router.execute() caught exception: " + e.getMessage());
        throw e;
    }

        return true;                          
    }

    static Pattern V1_INFO = Pattern.compile("^" + LegacyRoute.IIIF_1_PATH + "/([^/]+)/info\\.json$");
    static Pattern V2_INFO = Pattern.compile("^" + LegacyRoute.IIIF_2_PATH + "/([^/]+)/info\\.json$");
    static Pattern V3_INFO = Pattern.compile("^" + LegacyRoute.IIIF_3_PATH + "/([^/]+)/info\\.json$");
    

    static Route findControllerForRoute(String path) {
        if (path.equals("") || path.equals("/")) {
            return new Route(edu.illinois.library.cantaloupe.resource.LandingResource.class, null);
        } else if (path.endsWith("/")) {
            return new Route(edu.illinois.library.cantaloupe.resource.TrailingSlashRemovingResource.class, null);
        } else if (path.startsWith("/iiif/1")) {
            if (path.equals("/iiif/1")) {
                return new Route(edu.illinois.library.cantaloupe.resource.iiif.v1.LandingResource.class, null);
            }
        } else if (path.equals("/iiif/2")) {
            return new Route(edu.illinois.library.cantaloupe.resource.iiif.v2.LandingResource.class, null);
        } else if (path.equals("/iiif/3")) {
            return new Route(edu.illinois.library.cantaloupe.resource.iiif.v3.LandingResource.class, null);
        } else if (path.equals("/health")) {
            return new Route(edu.illinois.library.cantaloupe.resource.health.HealthResource.class, null);
        }
        return null;
    }
}