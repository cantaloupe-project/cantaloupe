package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import org.apache.commons.configuration.Configuration;
import org.apache.velocity.app.Velocity;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.service.StatusService;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.util.Map;

/**
 * Overrides the built-in Restlet response codes and status pages.
 */
class CantaloupeStatusService extends StatusService {

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