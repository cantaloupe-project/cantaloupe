package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.resource.ImageResource;
import edu.illinois.library.cantaloupe.resource.InformationResource;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsFilter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;

public class ImageServerApplication extends Application {

    public static final String BASE_IIIF_PATH = "/iiif";

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
            Throwable throwable = status.getThrowable();
            if (throwable.getCause() != null) {
                throwable = throwable.getCause();
            }

            String stackTrace = "";
            try {
                Configuration config = edu.illinois.library.cantaloupe.
                        Application.getConfiguration();
                if (config.getBoolean("print_stack_trace_on_error_page")) {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    stackTrace = sw.toString();
                }
            } catch (ConfigurationException e) {
                // nothing we can do
            }

            String template = "<html>" +
                    "<head></head>" +
                    "<body>" +
                    "<h1>%s %s</h1>" +
                    "<p>%s</p>" +
                    "<pre>%s</pre>" +
                    "</body>" +
                    "</html>";
            String msg = String.format(template,
                    Integer.toString(status.getCode()),
                    status.getReasonPhrase(), throwable.getMessage(),
                    stackTrace);
            return new StringRepresentation(msg, MediaType.TEXT_HTML,
                    Language.ENGLISH, CharacterSet.UTF_8);
        }

        @Override
        public Status getStatus(Throwable t, Request request,
                                Response response) {
            Status status;
            Throwable cause = t.getCause();
            if (cause instanceof IllegalArgumentException ||
                    cause instanceof UnsupportedEncodingException) {
                status = new Status(Status.CLIENT_ERROR_BAD_REQUEST, t);
            } else if (cause instanceof FileNotFoundException) {
                status = new Status(Status.CLIENT_ERROR_NOT_FOUND, t);
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
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        CorsFilter corsFilter = new CorsFilter(getContext(), router);
        corsFilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
        corsFilter.setAllowedCredentials(true);

        // 2 Redirect image identifier to image information
        // http://iiif.io/api/image/2.0/#uri-syntax
        // {scheme}://{server}{/prefix}/{identifier}
        Redirector redirector = new Redirector(getContext(),
                BASE_IIIF_PATH + "/{identifier}/info.json",
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(BASE_IIIF_PATH + "/{identifier}", redirector);

        // 2.1 Image Request
        // http://iiif.io/api/image/2.0/#image-request-uri-syntax
        // {scheme}://{server}{/prefix}/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
        router.attach(BASE_IIIF_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                ImageResource.class);

        // 5 Information Request
        // http://iiif.io/api/image/2.0/#information-request
        // {scheme}://{server}{/prefix}/{identifier}/info.json
        router.attach(BASE_IIIF_PATH + "/{identifier}/info.{format}",
                InformationResource.class);

        // landing page
        router.attach(BASE_IIIF_PATH, LandingResource.class);
        router.attach("/", LandingResource.class);

        return corsFilter;
    }

}