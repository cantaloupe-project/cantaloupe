package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resource.ImageResource;
import edu.illinois.library.cantaloupe.resource.InformationResource;
import edu.illinois.library.cantaloupe.resource.LandingResource;
import org.apache.commons.configuration.Configuration;
import org.apache.velocity.app.Velocity;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsFilter;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
            Map<String,Object> templateVars = new HashMap<String,Object>();
            templateVars.put("pageTitle", status.getCode() + " " +
                    status.getReasonPhrase());

            Throwable throwable = status.getThrowable();
            if (throwable != null) {
                templateVars.put("message", throwable.getMessage());
                if (throwable.getCause() != null) {
                    throwable = throwable.getCause();
                }
                Configuration config = edu.illinois.library.cantaloupe.
                        Application.getConfiguration();
                if (config.getBoolean("print_stack_trace_on_error_pages")) {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    templateVars.put("stackTrace", sw.toString());
                }
            }

            org.apache.velocity.Template template = Velocity.getTemplate("error.vm");
            return new TemplateRepresentation(template, templateVars,
                    MediaType.TEXT_HTML);
        }

        @Override
        // TODO: docs say to replace this with toStatus() but that doesn't get called
        public Status getStatus(Throwable t, Request request,
                                Response response) {
            Status status;
            Throwable cause = t.getCause();
            if (cause instanceof IllegalArgumentException ||
                    cause instanceof UnsupportedEncodingException) {
                status = new Status(Status.CLIENT_ERROR_BAD_REQUEST, t);
            } else if (cause instanceof UnsupportedSourceFormatException) {
                status = new Status(Status.CLIENT_ERROR_FORBIDDEN, t);
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
     *
     * @see <a href="http://iiif.io/api/image/2.0/#uri-syntax">URI Syntax</a>
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        CorsFilter corsFilter = new CorsFilter(getContext(), router);
        corsFilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
        corsFilter.setAllowedCredentials(true);

        // 2 Redirect image identifier to image information
        // {scheme}://{server}{/prefix}/{identifier}
        Redirector redirector = new Redirector(getContext(),
                BASE_IIIF_PATH + "/{identifier}/info.json",
                Redirector.MODE_CLIENT_SEE_OTHER);
        router.attach(BASE_IIIF_PATH + "/{identifier}", redirector);

        // 2.1 Image Request
        // {scheme}://{server}{/prefix}/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
        router.attach(BASE_IIIF_PATH + "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                ImageResource.class);

        // 5 Information Request
        // {scheme}://{server}{/prefix}/{identifier}/info.json
        router.attach(BASE_IIIF_PATH + "/{identifier}/info.{format}",
                InformationResource.class);

        // landing page
        router.attach(BASE_IIIF_PATH, LandingResource.class);
        router.attach("/", LandingResource.class);

        // Redirect / to BASE_IIIF_PATH
        redirector = new Redirector(getContext(), BASE_IIIF_PATH,
                Redirector.MODE_CLIENT_PERMANENT);
        router.attach("/", redirector);

        return corsFilter;
    }

}