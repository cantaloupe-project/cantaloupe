package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import org.apache.commons.configuration.Configuration;
import org.apache.velocity.app.Velocity;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsFilter;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Directory;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

public class ImageServerApplication extends Application {

    public static final String IIIF_PATH = "/iiif";
    public static final String IIIF_1_1_PATH = "/iiif/1.1";
    public static final String IIIF_2_0_PATH = "/iiif/2.0";
    public static final String STATIC_ROOT_PATH = "/static";

    /**
     * Overrides the built-in Restlet status pages. Converts the following
     * exceptions to the following HTTP statuses:
     *
     * <ul>
     *     <li>IllegalArgumentException: 400</li>
     *     <li>FileNotFoundException: 404</li>
     *     <li>Exception: 500</li>
     * </ul>
     */
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
                Configuration config = edu.illinois.library.cantaloupe.
                        Application.getConfiguration();
                if (config.getBoolean("print_stack_trace_on_error_pages")) {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    stackTrace = sw.toString();
                }
            } else if (status == Status.CLIENT_ERROR_NOT_FOUND) {
                message = "No resource exists at this URL.";
            }

            Map<String,Object> templateVars = new HashMap<>();
            templateVars.put("pageTitle", status.getCode() + " " +
                    status.getReasonPhrase());
            templateVars.put("message", message);
            templateVars.put("stackTrace", stackTrace);

            org.apache.velocity.Template template = Velocity.getTemplate("error.vm");
            return new TemplateRepresentation(template, templateVars,
                    MediaType.TEXT_HTML);
        }

        /**
         * Deprecated and replaced by <code>toStatus()</code>, but that doesn't
         * get called due to a bug in Restlet as of version 2.3.5.
         *
         * @param t
         * @param request
         * @param response
         * @return
         * @see https://github.com/restlet/restlet-framework-java/issues/1156#issuecomment-145449634
         */
        @Override
        @SuppressWarnings({"deprecation"})
        public Status getStatus(Throwable t, Request request,
                                Response response) {
            return toStatus(t, request, response);
        }

        @Override
        public Status toStatus(Throwable t, Request request,
                               Response response) {
            Status status;
            Throwable cause = t.getCause();
            if (cause instanceof IllegalArgumentException ||
                    cause instanceof UnsupportedEncodingException ||
                    cause instanceof UnsupportedOutputFormatException) {
                status = new Status(Status.CLIENT_ERROR_BAD_REQUEST, t);
            } else if (cause instanceof UnsupportedSourceFormatException) {
                status = new Status(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, t);
            } else if (cause instanceof FileNotFoundException) {
                status = new Status(Status.CLIENT_ERROR_NOT_FOUND, t);
            } else if (cause instanceof AccessDeniedException) {
                status = new Status(Status.CLIENT_ERROR_FORBIDDEN, t);
            } else {
                status = new Status(Status.SERVER_ERROR_INTERNAL, t);
            }
            return status;
        }

    }

    public ImageServerApplication() {
        super();
        this.setStatusService(new CustomStatusService());
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     *
     * @see <a href="http://iiif.io/api/image/2.0/#uri-syntax">URI Syntax</a>
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        CorsFilter corsFilter = new CorsFilter(getContext(), router);
        corsFilter.setAllowedOrigins(new HashSet<>(Arrays.asList("*")));
        corsFilter.setAllowedCredentials(true);

        /****************** IIIF Image API 1.1 routes *******************/

        // Redirect image identifier to image information
        Redirector redirector = new Redirector(getContext(),
                IIIF_1_1_PATH + "/{identifier}/info.json",
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(IIIF_1_1_PATH + "/{identifier}", redirector);

        // Redirect IIIF_1_1_PATH/ to IIIF_1_1_PATH
        redirector = new Redirector(getContext(), IIIF_1_1_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_1_1_PATH + "/", redirector);

        // image request
        Class resource = edu.illinois.library.cantaloupe.resource.iiif.v1_1.ImageResource.class;
        router.attach(IIIF_1_1_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                resource);

        // information request
        resource = edu.illinois.library.cantaloupe.resource.iiif.v1_1.InformationResource.class;
        router.attach(IIIF_1_1_PATH + "/{identifier}/info.{format}", resource);

        /****************** IIIF Image API 2.0 routes *******************/

        // Redirect image identifier to image information
        redirector = new Redirector(getContext(),
                IIIF_2_0_PATH + "/{identifier}/info.json",
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(IIIF_2_0_PATH + "/{identifier}", redirector);

        // Redirect IIIF_2_0_PATH/ to IIIF_2_0_PATH
        redirector = new Redirector(getContext(), IIIF_2_0_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_2_0_PATH + "/", redirector);

        // image request
        resource = edu.illinois.library.cantaloupe.resource.iiif.v2_0.ImageResource.class;
        router.attach(IIIF_2_0_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                resource);

        // information request
        resource = edu.illinois.library.cantaloupe.resource.iiif.v2_0.InformationResource.class;
        router.attach(IIIF_2_0_PATH + "/{identifier}/info.{format}", resource);

        // Redirect IIIF_PATH to IIIF_2_0_PATH (designates 2.0 as the default
        // IIIF Image API)
        redirector = new Redirector(getContext(), IIIF_2_0_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach(IIIF_PATH, redirector);

        /****************** Other routes *******************/

        // landing page
        router.attach("/", LandingResource.class);

        // Hook up HTTP Basic authentication
        try {
            Configuration config = edu.illinois.library.cantaloupe.Application.
                    getConfiguration();
            if (config.getBoolean("http.auth.basic")) {
                ChallengeAuthenticator authenticator = new ChallengeAuthenticator(
                        getContext(), ChallengeScheme.HTTP_BASIC,
                        "Cantaloupe Realm");
                MapVerifier verifier = new MapVerifier();
                verifier.getLocalSecrets().put(
                        config.getString("http.auth.basic.username"),
                        config.getString("http.auth.basic.secret").toCharArray());
                authenticator.setVerifier(verifier);
                authenticator.setNext(corsFilter);
                return authenticator;
            }
        } catch (NoSuchElementException e) {
            getLogger().info("HTTP Basic authentication disabled.");
        }

        // Hook up the static file server (for CSS & images)
        final Directory dir = new Directory(
                getContext(), "clap://resources/public_html/");
        dir.setDeeplyAccessible(true);
        dir.setListingAllowed(false);
        dir.setNegotiatingContent(false);
        router.attach(STATIC_ROOT_PATH, dir);

        return corsFilter;
    }

}