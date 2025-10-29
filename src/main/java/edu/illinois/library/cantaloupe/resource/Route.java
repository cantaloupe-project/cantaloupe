package edu.illinois.library.cantaloupe.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Associates a URI path pattern with an {@link AbstractResource}
 * implementation.
 *
 * @since 4.1
 */
public final class Route {

    public static final String ADMIN_PATH         = "/admin";
    public static final String IIIF_1_PATH        = "/iiif/1";
    public static final String IIIF_2_PATH        = "/iiif/2";
    public static final String IIIF_3_PATH        = "/iiif/3";

    /**
     * N.B.: the {@link LinkedHashMap} preserves order as each mapping will be
     * checked sequentially and the first match used.
     */
    private static final Map<Pattern,RouteEntry> MAPPINGS =
            new LinkedHashMap<>();

    private static class RouteEntry {
        Class<? extends AbstractResource> resource;
        Class<? extends Request> request;

        RouteEntry(Class<? extends AbstractResource> resource, Class<? extends Request> request) {
            this.request = request;
            this.resource = resource;
        }

        public Class<? extends AbstractResource> getResourceClass() {
            return resource;
        }

        public Class<? extends Request> getRequestClass() {
            return request;
        }       
    }
    private Class<? extends AbstractResource> resource;
    private Class<? extends Request> request;

    private final List<String> pathArguments = new ArrayList<>();

    static {

        // N.B.: Regex groups are used to extract the URI path arguments.
        MAPPINGS.put(Pattern.compile("/$"),
                new RouteEntry(TrailingSlashRemovingResource.class, Request.class));

        // IIIF Image API v1 routes
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/.]+)$"),
                new RouteEntry(edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class, IIIFRequest.class));
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/.]+)\\.([^/]+)$"),
                new RouteEntry(edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class, IIIFRequest.class));
    }

    /**
     * @param path URI path relative to the context path.
     * @return     Route corresponding to the given path, or {@code null} if
     *             there is no match.
     */
    static Route forPath(String path) {
        for (var entry : MAPPINGS.entrySet()) {
            final Pattern pattern = entry.getKey();
            final Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                RouteEntry routeEntry = entry.getValue();
                final Route route = new Route(routeEntry.getResourceClass(), routeEntry.getRequestClass());
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    route.getPathArguments().add(matcher.group(i));
                }
                return route;
            }
        }
        return null;
    }

    /**
     * <p>Returns a list of non-decoded URI path components that are considered
     * arguments, as extracted from the string argument to {@link
     * #forPath(String)}. For example, this URI path has six arguments:</p>
     *
     * <p>{@code /iiif/2/[identifier]/[region]/[size]/[rotation]/[quality].[format]}</p>
     */
    List<String> getPathArguments() {
        return pathArguments;
    }

    Route(Class<? extends AbstractResource> resource, Class<? extends Request> request) {
        this.resource = resource;
        this.request = request;
    }

    /**
     * @return Resource the instance "connects" to.
     */
    Class<? extends AbstractResource> getResource() {
        return resource;
    }

    /**
     * @return Request the users intent
     */
    Class<? extends Request> getRequest() {
        return request;
    }
}
