package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.logging.velocity.Slf4jLogChute;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import edu.illinois.library.cantaloupe.resource.admin.AdminResource;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.velocity.TemplateRepresentation;
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
 */
public class WebApplication extends Application {

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
                Configuration config = Configuration.getInstance();
                if (config.getBoolean("print_stack_trace_on_error_pages", false)) {
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

            org.apache.velocity.Template template = Velocity.getTemplate("error.vm");
            return new TemplateRepresentation(template, templateVars,
                    MediaType.TEXT_HTML);
        }

        @Override
        public Status toStatus(Throwable t, Request request,
                               Response response) {
            Status status;
            t = (t.getCause() != null) ? t.getCause() : t;

            if (t instanceof ResourceException) {
                status = ((ResourceException) t).getStatus();
            } else if (t instanceof IllegalArgumentException ||
                    t instanceof UnsupportedEncodingException ||
                    t instanceof UnsupportedOutputFormatException) {
                status = new Status(Status.CLIENT_ERROR_BAD_REQUEST, t);
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

    public static final String ADMIN_SECRET_CONFIG_KEY = "admin.password";
    public static final String BASIC_AUTH_ENABLED_CONFIG_KEY =
            "auth.basic.enabled";
    public static final String BASIC_AUTH_SECRET_CONFIG_KEY =
            "auth.basic.secret";
    public static final String BASIC_AUTH_USERNAME_CONFIG_KEY =
            "auth.basic.username";

    public static final String ADMIN_PATH = "/admin";
    public static final String IIIF_PATH = "/iiif";
    public static final String IIIF_1_PATH = "/iiif/1";
    public static final String IIIF_2_PATH = "/iiif/2";
    public static final String STATIC_ROOT_PATH = "/static";

    static {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        Velocity.setProperty("runtime.log.logsystem.class",
                Slf4jLogChute.class.getCanonicalName());
        Velocity.init();
    }

    public WebApplication() {
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
        final Configuration config = Configuration.getInstance();
        final String secret = config.getString(ADMIN_SECRET_CONFIG_KEY);
        if (secret == null || secret.length() < 1) {
            throw new ConfigurationException(
                    ADMIN_SECRET_CONFIG_KEY + " is not set. The control " +
                            "panel will be unavailable.");
        }

        final MapVerifier verifier = new MapVerifier();
        verifier.getLocalSecrets().put("admin", secret.toCharArray());
        final ChallengeAuthenticator auth = new ChallengeAuthenticator(
                getContext(), ChallengeScheme.HTTP_BASIC, "Cantaloupe Realm");
        auth.setVerifier(verifier);
        return auth;
    }

    private ChallengeAuthenticator createEndpointAuthenticator() {
        final Configuration config = Configuration.getInstance();
        final String username = config.getString(BASIC_AUTH_USERNAME_CONFIG_KEY);
        final String secret = config.getString(BASIC_AUTH_SECRET_CONFIG_KEY);

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
                    if (config.getBoolean(BASIC_AUTH_ENABLED_CONFIG_KEY, false)) {
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
                BASIC_AUTH_USERNAME_CONFIG_KEY + " or " +
                BASIC_AUTH_SECRET_CONFIG_KEY + " are null)");
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
            getLogger().log(Level.SEVERE, e.getMessage());
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