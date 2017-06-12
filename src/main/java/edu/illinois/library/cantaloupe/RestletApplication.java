package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import edu.illinois.library.cantaloupe.resource.admin.AdminResource;
import edu.illinois.library.cantaloupe.resource.api.CacheResource;
import edu.illinois.library.cantaloupe.resource.api.ConfigurationResource;
import edu.illinois.library.cantaloupe.resource.api.DMICResource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Directory;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
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
 * them to Resources.
 *
 * @see <a href="https://restlet.com/open-source/documentation/jdocs/2.3/jse">
 *     Restlet JSE Javadoc</a>
 */
public class RestletApplication extends Application {

    private class CustomStatusService extends StatusService {

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
                Configuration config = ConfigurationFactory.getInstance();
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
    public static final String CACHE_PATH = "/cache";
    public static final String CONFIGURATION_PATH = "/configuration";
    public static final String DELEGATE_METHOD_INVOCATION_CACHE_PATH = "/dmic";
    public static final String IIIF_PATH = "/iiif";
    public static final String IIIF_1_PATH = "/iiif/1";
    public static final String IIIF_2_PATH = "/iiif/2";
    public static final String STATIC_ROOT_PATH = "/static";

    public RestletApplication() {
        super();
        this.setStatusService(new CustomStatusService());
        // http://restlet.com/blog/2015/12/15/understanding-and-using-cors/
        CorsService corsService = new CorsService();
        corsService.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
        corsService.setAllowedCredentials(true);
        this.getServices().add(corsService);
    }

    private ChallengeAuthenticator createAdminAuthenticator()
            throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String secret = config.getString(Key.ADMIN_SECRET);
        if (secret == null || secret.length() < 1) {
            throw new ConfigurationException(Key.ADMIN_SECRET +
                    " is not set. The control panel will be unavailable.");
        }

        final MapVerifier verifier = new MapVerifier();
        verifier.getLocalSecrets().put("admin", secret.toCharArray());
        final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                getContext(), ChallengeScheme.HTTP_BASIC,
                "Cantaloupe Control Panel");
        auth.setVerifier(verifier);
        return auth;
    }

    private ChallengeAuthenticator createApiAuthenticator()
            throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String secret = config.getString(Key.API_SECRET);
        if (secret == null || secret.length() < 1) {
            throw new ConfigurationException(Key.API_SECRET +
                    " is not set. The API will be unavailable.");
        }

        final MapVerifier verifier = new MapVerifier();
        verifier.getLocalSecrets().put(config.getString(Key.API_USERNAME),
                secret.toCharArray());
        final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                getContext(), ChallengeScheme.HTTP_BASIC,
                "Cantaloupe API Realm");
        auth.setVerifier(verifier);
        return auth;
    }

    private ChallengeAuthenticator createEndpointAuthenticator() {
        final Configuration config = ConfigurationFactory.getInstance();
        final String username = config.getString(Key.BASIC_AUTH_USERNAME);
        final String secret = config.getString(Key.BASIC_AUTH_SECRET);

        if (username != null && username.length() > 0 && secret != null &&
                secret.length() > 0) {
            getLogger().log(Level.INFO,
                    "Enabling HTTP Basic authentication for all endpoints");
            final MapVerifier verifier = new MapVerifier();
            verifier.getLocalSecrets().put(username, secret.toCharArray());
            final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                    getContext(), ChallengeScheme.HTTP_BASIC, "Image Realm") {
                @Override
                protected int beforeHandle(Request request, Response response) {
                    if (config.getBoolean(Key.BASIC_AUTH_ENABLED, false)) {
                        if (!request.getResourceRef().getPath().startsWith(ADMIN_PATH) &&
                                !request.getResourceRef().getPath().startsWith(STATIC_ROOT_PATH)) {
                            return super.beforeHandle(request, response);
                        }
                    }
                    response.setStatus(Status.SUCCESS_OK);
                    return CONTINUE;
                }
            };
            auth.setVerifier(verifier);
            return auth;
        }
        getLogger().log(Level.INFO, "Endpoint authentication is disabled. (" +
                Key.BASIC_AUTH_USERNAME + " or " + Key.BASIC_AUTH_SECRET +
                " are null)");
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

        /****************** IIIF Image API 1.1 routes *******************/

        // landing page
        router.attach(IIIF_1_PATH,
                edu.illinois.library.cantaloupe.resource.iiif.v1.LandingResource.class);

        // Redirect IIIF_1_PATH/ to IIIF_1_PATH
        Redirector redirector = new Redirector(getContext(), IIIF_1_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_1_PATH + "/", redirector);

        // image request
        router.attach(IIIF_1_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality_format}",
                edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class);

        // information request
        router.attach(IIIF_1_PATH + "/{identifier}",
                edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.RedirectingResource.class);
        router.attach(IIIF_1_PATH + "/{identifier}/info.json",
                edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class);

        /****************** IIIF Image API 2.0 routes *******************/

        // landing page
        router.attach(IIIF_2_PATH,
                edu.illinois.library.cantaloupe.resource.iiif.v2.LandingResource.class);

        // Redirect IIIF_2_PATH/ to IIIF_2_PATH
        redirector = new Redirector(getContext(), IIIF_2_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_2_PATH + "/", redirector);

        // image request
        router.attach(IIIF_2_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class);

        // information request
        router.attach(IIIF_2_PATH + "/{identifier}",
                edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.RedirectingResource.class);
        router.attach(IIIF_2_PATH + "/{identifier}/info.json",
                edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.class);

        // 303-redirect IIIF_PATH to IIIF_2_PATH
        redirector = new Redirector(getContext(), IIIF_2_PATH,
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(IIIF_PATH, redirector);

        /****************** Admin route ********************/

        try {
            ChallengeAuthenticator adminAuth = createAdminAuthenticator();
            adminAuth.setNext(AdminResource.class);
            router.attach(ADMIN_PATH, adminAuth);
        } catch (ConfigurationException e) {
            getLogger().log(Level.WARNING, e.getMessage());
        }

        /****************** API routes ********************/

        try {
            ChallengeAuthenticator apiAuth = createApiAuthenticator();
            apiAuth.setNext(ConfigurationResource.class);
            router.attach(CONFIGURATION_PATH, apiAuth);

            apiAuth = createApiAuthenticator();
            apiAuth.setNext(CacheResource.class);
            router.attach(CACHE_PATH + "/{identifier}", apiAuth);

            apiAuth = createApiAuthenticator();
            apiAuth.setNext(DMICResource.class);
            router.attach(DELEGATE_METHOD_INVOCATION_CACHE_PATH, apiAuth);
        } catch (ConfigurationException e) {
            getLogger().log(Level.INFO, e.getMessage());
        }

        /****************** Other routes *******************/

        // landing page
        router.attach("/", LandingResource.class);

        // Hook up the static file server (for CSS & images)
        final Directory dir = new Directory(
                getContext(), "clap://resources/public_html/");
        dir.setDeeplyAccessible(true);
        dir.setListingAllowed(false);
        dir.setNegotiatingContent(false);
        router.attach(STATIC_ROOT_PATH, dir);

        // Hook up endpoint authentication
        ChallengeAuthenticator endpointAuth = createEndpointAuthenticator();
        if (endpointAuth != null) {
            endpointAuth.setNext(router);
            return endpointAuth;
        }
        return router;
    }

}