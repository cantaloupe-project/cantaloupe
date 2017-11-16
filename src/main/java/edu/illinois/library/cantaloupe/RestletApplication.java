package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import edu.illinois.library.cantaloupe.resource.TrailingSlashRemovingResource;
import edu.illinois.library.cantaloupe.resource.admin.AdminResource;
import edu.illinois.library.cantaloupe.resource.api.CacheResource;
import edu.illinois.library.cantaloupe.resource.api.TaskResource;
import edu.illinois.library.cantaloupe.resource.api.TasksResource;
import edu.illinois.library.cantaloupe.resource.iiif.RedirectingResource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Directory;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.restlet.service.CorsService;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

/**
 * Restlet Application implementation. Creates endpoint routes and connects
 * them to {@link org.restlet.resource.Resource resources}.
 *
 * @see <a href="https://restlet.com/open-source/documentation/jdocs/2.3/jse">
 *     Restlet 2.3 JSE Javadoc</a>
 * @see <a href="https://restlet.com/open-source/documentation/2.3/changelog">
 *     Restlet Change Log</a>
 */
public class RestletApplication extends Application {

    private static class CustomStatusService extends StatusService {

        @Override
        public Representation toRepresentation(Status status, Request request,
                                               Response response) {
            String message = null, stackTrace = null;
            Throwable throwable = status.getThrowable();
            if (throwable != null) {
                if (throwable.getCause() != null) {
                    throwable = throwable.getCause();
                }
                message = throwable.getMessage();
                Configuration config = Configuration.getInstance();
                if (config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false)) {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    stackTrace = sw.toString();
                }
            } else if (status.getDescription() != null) {
                message = status.getDescription();
            } else if (status == Status.CLIENT_ERROR_NOT_FOUND) {
                message = "No resource exists at this URL.";
            }

            final Map<String,Object> templateVars =
                    AbstractResource.getCommonTemplateVars(request);
            templateVars.put("pageTitle", status.getCode() + " " +
                    status.getReasonPhrase());
            templateVars.put("message", message);
            templateVars.put("stackTrace", stackTrace);

            class AnonymousResource extends AbstractResource {}
            return new AnonymousResource().template("/error.vm", templateVars);
        }

        @Override
        public Status toStatus(Throwable t, Request request,
                               Response response) {
            Status status;
            t = (t.getCause() != null) ? t.getCause() : t;

            if (t instanceof ResourceException) {
                status = ((ResourceException) t).getStatus();
            } else if (t instanceof IllegalArgumentException ||
                    t instanceof ValidationException ||
                    t instanceof UnsupportedEncodingException) {
                status = new Status(Status.CLIENT_ERROR_BAD_REQUEST, t);
            } else if (t instanceof UnsupportedOutputFormatException) {
                status = new Status(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, t);
            } else if (t instanceof FileNotFoundException) {
                status = new Status(Status.CLIENT_ERROR_NOT_FOUND, t);
            } else if (t instanceof AccessDeniedException) {
                status = new Status(Status.CLIENT_ERROR_FORBIDDEN, t);
            } else {
                status = new Status(Status.SERVER_ERROR_INTERNAL, t);
            }
            return status;
        }

    }

    public static final String ADMIN_PATH = "/admin";
    public static final String ADMIN_CONFIG_PATH = "/admin/configuration";
    public static final String CACHE_PATH = "/cache";
    public static final String CONFIGURATION_PATH = "/configuration";
    public static final String IIIF_PATH = "/iiif";
    public static final String IIIF_1_PATH = "/iiif/1";
    public static final String IIIF_2_PATH = "/iiif/2";
    public static final String STATIC_ROOT_PATH = "/static";
    public static final String TASKS_PATH = "/tasks";

    public static final String ADMIN_REALM = "Cantaloupe Control Panel";
    public static final String API_REALM = "Cantaloupe API Realm";
    public static final String PUBLIC_REALM = "Image Realm";

    public RestletApplication() {
        super();
        setStatusService(new CustomStatusService());
        // http://restlet.com/blog/2015/12/15/understanding-and-using-cors/
        CorsService corsService = new CorsService();
        corsService.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
        corsService.setAllowedCredentials(true);
        getServices().add(corsService);
    }

    private ChallengeAuthenticator newAdminAuthenticator()
            throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String secret = config.getString(Key.ADMIN_SECRET);
        if (secret == null || secret.isEmpty()) {
            throw new ConfigurationException(Key.ADMIN_SECRET +
                    " is not set. The Control Panel will be unavailable.");
        }

        final MapVerifier verifier = new MapVerifier();
        verifier.getLocalSecrets().put("admin", secret.toCharArray());
        final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                getContext(), ChallengeScheme.HTTP_BASIC, ADMIN_REALM);
        auth.setVerifier(verifier);
        return auth;
    }

    private ChallengeAuthenticator newAPIAuthenticator()
            throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String secret = config.getString(Key.API_SECRET);
        if (secret == null || secret.isEmpty()) {
            throw new ConfigurationException(Key.API_SECRET +
                    " is not set. The API will be unavailable.");
        }

        final MapVerifier verifier = new MapVerifier();
        verifier.getLocalSecrets().put(config.getString(Key.API_USERNAME),
                secret.toCharArray());
        final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                getContext(), ChallengeScheme.HTTP_BASIC, API_REALM);
        auth.setVerifier(verifier);
        return auth;
    }

    private ChallengeAuthenticator newPublicEndpointAuthenticator()
            throws ConfigurationException {
        final Configuration config = Configuration.getInstance();

        if (config.getBoolean(Key.BASIC_AUTH_ENABLED, false)) {
            final String username = config.getString(Key.BASIC_AUTH_USERNAME, "");
            final String secret = config.getString(Key.BASIC_AUTH_SECRET, "");

            if (username != null && !username.isEmpty() && secret != null
                    && !secret.isEmpty()) {
                getLogger().log(Level.INFO,
                        "Enabling HTTP Basic authentication for public endpoints");

                final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                        getContext(), ChallengeScheme.HTTP_BASIC, PUBLIC_REALM) {
                    @Override
                    protected int beforeHandle(Request request, Response response) {
                        final String path = request.getResourceRef().getPath();
                        if (path.startsWith(IIIF_PATH)) {
                            return super.beforeHandle(request, response);
                        }
                        response.setStatus(Status.SUCCESS_OK);
                        return CONTINUE;
                    }
                };

                final MapVerifier verifier = new MapVerifier();
                verifier.getLocalSecrets().put(username, secret.toCharArray());

                auth.setVerifier(verifier);
                return auth;
            } else {
                throw new ConfigurationException("Public endpoint " +
                        "authentication is enabled in the configuration, but " +
                        Key.BASIC_AUTH_USERNAME + " and/or " +
                        Key.BASIC_AUTH_SECRET + " are not set");
            }
        } else {
            getLogger().info("Public endpoint authentication is disabled (" +
                    Key.BASIC_AUTH_ENABLED + " = false)");
        }
        return null;
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     *
     * @see <a href="http://iiif.io/api/image/2.0/#uri-syntax">URI Syntax</a>
     */
    @Override
    public Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        ////////////////////// IIIF Image API routes ///////////////////////

        // Redirect IIIF_PATH/ to IIIF_PATH
        router.attach(IIIF_PATH + "/", TrailingSlashRemovingResource.class);

        // Redirect IIIF_PATH to IIIF_2_PATH
        router.attach(IIIF_PATH, RedirectingResource.class);

        //////////////////// IIIF Image API 1.x routes /////////////////////

        // landing page
        router.attach(IIIF_1_PATH,
                edu.illinois.library.cantaloupe.resource.iiif.v1.LandingResource.class);

        // Redirect IIIF_1_PATH/ to IIIF_1_PATH
        router.attach(IIIF_1_PATH + "/", TrailingSlashRemovingResource.class);

        // image request
        router.attach(IIIF_1_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality_format}",
                edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class);

        // information request
        router.attach(IIIF_1_PATH + "/{identifier}",
                edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.RedirectingResource.class);
        router.attach(IIIF_1_PATH + "/{identifier}/info.json",
                edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class);

        //////////////////// IIIF Image API 2.x routes /////////////////////

        // landing page
        router.attach(IIIF_2_PATH,
                edu.illinois.library.cantaloupe.resource.iiif.v2.LandingResource.class);

        // Redirect IIIF_2_PATH/ to IIIF_2_PATH
        router.attach(IIIF_2_PATH + "/", TrailingSlashRemovingResource.class);

        // image request
        router.attach(IIIF_2_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class);

        // information request
        router.attach(IIIF_2_PATH + "/{identifier}",
                edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.RedirectingResource.class);
        router.attach(IIIF_2_PATH + "/{identifier}/info.json",
                edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.class);

        ////////////////////////// Admin routes ///////////////////////////

        try {
            ChallengeAuthenticator adminAuth = newAdminAuthenticator();
            adminAuth.setNext(AdminResource.class);
            router.attach(ADMIN_PATH, adminAuth);

            adminAuth = newAdminAuthenticator();
            adminAuth.setNext(edu.illinois.library.cantaloupe.resource.admin.ConfigurationResource.class);
            router.attach(ADMIN_CONFIG_PATH, adminAuth);
        } catch (ConfigurationException e) {
            getLogger().log(Level.INFO, e.getMessage());
        }

        /////////////////////////// API routes ////////////////////////////

        try {
            ChallengeAuthenticator apiAuth = newAPIAuthenticator();
            apiAuth.setNext(edu.illinois.library.cantaloupe.resource.api.ConfigurationResource.class);
            router.attach(CONFIGURATION_PATH, apiAuth);

            apiAuth = newAPIAuthenticator();
            apiAuth.setNext(CacheResource.class);
            router.attach(CACHE_PATH + "/{identifier}", apiAuth);

            apiAuth = newAPIAuthenticator();
            apiAuth.setNext(TasksResource.class);
            router.attach(TASKS_PATH, apiAuth);

            apiAuth = newAPIAuthenticator();
            apiAuth.setNext(TaskResource.class);
            router.attach(TASKS_PATH + "/{uuid}", apiAuth);
        } catch (ConfigurationException e) {
            getLogger().log(Level.INFO, e.getMessage());
        }

        ////////////////////////// Other routes ///////////////////////////

        // Application landing page
        router.attach("/", LandingResource.class);

        // Hook up the static file server (for images, CSS, & scripts)
        // This uses Restlet's "CLAP" (Class Loader Access Protocol) mechanism
        // which must be enabled in web.xml.
        final Directory dir = new Directory(
                getContext(), "clap://resources/public_html/");
        dir.setDeeplyAccessible(true);
        dir.setListingAllowed(false);
        dir.setNegotiatingContent(false);
        router.attach(STATIC_ROOT_PATH, dir);

        // Hook up IIIF endpoint authentication
        try {
            ChallengeAuthenticator endpointAuth = newPublicEndpointAuthenticator();
            if (endpointAuth != null) {
                endpointAuth.setNext(router);
                return endpointAuth;
            }
        } catch (ConfigurationException e) {
            getLogger().log(Level.WARNING, e.getMessage());
        }
        return router;
    }

}
