package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.resource.LandingResource;
import org.apache.commons.configuration.Configuration;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.engine.application.CorsFilter;
import org.restlet.resource.Directory;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 * Restlet Application implementation. Creates endpoint routes and connects
 * them to Resources.
 */
public class WebApplication extends Application {

    public static final String BASIC_AUTH_ENABLED_CONFIG_KEY =
            "auth.basic.enabled";
    public static final String BASIC_AUTH_SECRET_CONFIG_KEY =
            "auth.basic.secret";
    public static final String BASIC_AUTH_USERNAME_CONFIG_KEY =
            "auth.basic.username";

    public static final String IIIF_PATH = "/iiif";
    public static final String IIIF_1_PATH = "/iiif/1";
    public static final String IIIF_2_PATH = "/iiif/2";
    public static final String STATIC_ROOT_PATH = "/static";

    public WebApplication() {
        super();
        this.setStatusService(new CantaloupeStatusService());
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

        CorsFilter corsFilter = new CorsFilter(getContext(), router);
        corsFilter.setAllowedOrigins(new HashSet<>(Arrays.asList("*")));
        corsFilter.setAllowedCredentials(true);

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
        Class<? extends ServerResource> resource =
                edu.illinois.library.cantaloupe.resource.iiif.v1.ImageResource.class;
        router.attach(IIIF_1_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality_format}",
                resource);

        // information request
        resource = edu.illinois.library.cantaloupe.resource.iiif.v1.InformationResource.class;
        router.attach(IIIF_1_PATH + "/{identifier}/info.{format}", resource);

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
        resource = edu.illinois.library.cantaloupe.resource.iiif.v2.ImageResource.class;
        router.attach(IIIF_2_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                resource);

        // information request
        resource = edu.illinois.library.cantaloupe.resource.iiif.v2.InformationResource.class;
        router.attach(IIIF_2_PATH + "/{identifier}/info.{format}", resource);

        // 303-redirect IIIF_PATH to IIIF_2_PATH
        redirector = new Redirector(getContext(), IIIF_2_PATH,
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(IIIF_PATH, redirector);

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

        // Hook up HTTP Basic authentication
        try {
            Configuration config = edu.illinois.library.cantaloupe.Application.
                    getConfiguration();
            if (config.getBoolean(BASIC_AUTH_ENABLED_CONFIG_KEY)) {
                ChallengeAuthenticator authenticator = new ChallengeAuthenticator(
                        getContext(), ChallengeScheme.HTTP_BASIC,
                        "Cantaloupe Realm");
                MapVerifier verifier = new MapVerifier();
                verifier.getLocalSecrets().put(
                        config.getString(BASIC_AUTH_USERNAME_CONFIG_KEY),
                        config.getString(BASIC_AUTH_SECRET_CONFIG_KEY).toCharArray());
                authenticator.setVerifier(verifier);
                authenticator.setNext(corsFilter);
                return authenticator;
            }
        } catch (NoSuchElementException e) {
            getLogger().info("HTTP Basic authentication disabled.");
        }

        return corsFilter;
    }

}