package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import edu.illinois.library.cantaloupe.resource.admin.AdminResource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.resource.Directory;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.restlet.service.CorsService;

import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Restlet Application implementation. Creates endpoint routes and connects
 * them to Resources.
 */
public class WebApplication extends Application {

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

    public WebApplication() {
        super();
        this.setStatusService(new CantaloupeStatusService());
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

        // Redirect image identifier to image information
        Redirector redirector = new Redirector(getContext(),
                IIIF_1_PATH + "/{identifier}/info.json",
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(IIIF_1_PATH + "/{identifier}", redirector);

        // Redirect IIIF_1_PATH/ to IIIF_1_PATH
        redirector = new Redirector(getContext(), IIIF_1_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_1_PATH + "/", redirector);

        // image request
        router.attach(IIIF_1_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality_format}",
                edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class);

        // information request
        router.attach(IIIF_1_PATH + "/{identifier}/info.{format}",
                edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class);

        /****************** IIIF Image API 2.0 routes *******************/

        // landing page
        router.attach(IIIF_2_PATH,
                edu.illinois.library.cantaloupe.resource.iiif.v2.LandingResource.class);

        // Redirect image identifier to image information
        redirector = new Redirector(getContext(),
                IIIF_2_PATH + "/{identifier}/info.json",
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(IIIF_2_PATH + "/{identifier}", redirector);

        // Redirect IIIF_2_PATH/ to IIIF_2_PATH
        redirector = new Redirector(getContext(), IIIF_2_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_2_PATH + "/", redirector);

        // image request
        router.attach(IIIF_2_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class);

        // information request
        router.attach(IIIF_2_PATH + "/{identifier}/info.{format}",
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