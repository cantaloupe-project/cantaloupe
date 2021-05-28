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
    public static final String ADMIN_CONFIG_PATH  = "/admin/configuration";
    public static final String ADMIN_STATUS_PATH  = "/admin/status";
    public static final String CONFIGURATION_PATH = "/configuration";
    public static final String HEALTH_PATH        = "/health";
    public static final String IIIF_1_PATH        = "/iiif/1";
    public static final String IIIF_2_PATH        = "/iiif/2";
    public static final String IIIF_3_PATH        = "/iiif/3";
    public static final String STATUS_PATH        = "/status";
    public static final String TASKS_PATH         = "/tasks";

    /**
     * N.B.: the {@link LinkedHashMap} preserves order as each mapping will be
     * checked sequentially and the first match used.
     */
    private static final Map<Pattern,Class<? extends AbstractResource>> MAPPINGS =
            new LinkedHashMap<>();

    private Class<? extends AbstractResource> resource;
    private final List<String> pathArguments = new ArrayList<>();

    static {
        // N.B.: Regex groups are used to extract the URI path arguments.
        MAPPINGS.put(Pattern.compile("\\A\\z"),
                LandingResource.class);
        MAPPINGS.put(Pattern.compile("^/$"),
                LandingResource.class);
        MAPPINGS.put(Pattern.compile("/$"),
                TrailingSlashRemovingResource.class);

        // IIIF Image API v3 routes
        MAPPINGS.put(Pattern.compile("^" + IIIF_3_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.iiif.v3.LandingResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_3_PATH + "/([^/]+)/info\\.json$"),
                edu.illinois.library.cantaloupe.resource.iiif.v3.InformationResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_3_PATH + "/([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v3.IdentifierResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_3_PATH + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)\\.([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v3.ImageResource.class);

        // IIIF Image API v2 routes
        MAPPINGS.put(Pattern.compile("^" + IIIF_2_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.iiif.v2.LandingResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_2_PATH + "/([^/]+)/info\\.json$"),
                edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_2_PATH + "/([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v2.IdentifierResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_2_PATH + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)\\.([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class);

        // IIIF Image API v1 routes
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.iiif.v1.LandingResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "/([^/]+)/info\\.json$"),
                edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "/([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v1.IdentifierResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/.]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class);
        MAPPINGS.put(Pattern.compile("^" + IIIF_1_PATH + "/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/.]+)\\.([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class);

        // Control Panel routes
        MAPPINGS.put(Pattern.compile("^" + ADMIN_CONFIG_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.admin.ConfigurationResource.class);
        MAPPINGS.put(Pattern.compile("^" + ADMIN_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.admin.AdminResource.class);
        MAPPINGS.put(Pattern.compile("^" + ADMIN_STATUS_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.admin.StatusResource.class);

        // API routes
        MAPPINGS.put(Pattern.compile("^" + CONFIGURATION_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.api.ConfigurationResource.class);
        MAPPINGS.put(Pattern.compile("^" + HEALTH_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.api.HealthResource.class);
        MAPPINGS.put(Pattern.compile("^" + STATUS_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.api.StatusResource.class);
        MAPPINGS.put(Pattern.compile("^" + TASKS_PATH + "$"),
                edu.illinois.library.cantaloupe.resource.api.TasksResource.class);
        MAPPINGS.put(Pattern.compile("^" + TASKS_PATH + "/([^/]+)$"),
                edu.illinois.library.cantaloupe.resource.api.TaskResource.class);
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
                final Route route = new Route();
                route.setResource(entry.getValue());
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

    /**
     * @return Resource the instance "connects" to.
     */
    Class<? extends AbstractResource> getResource() {
        return resource;
    }

    void setResource(Class<? extends AbstractResource> resource) {
        this.resource = resource;
    }

}
