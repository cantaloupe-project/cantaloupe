package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Translates a {@link Throwable} to an HTTP 4xx or 5xx-level response.
 */
class ErrorResource extends AbstractResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ErrorResource.class);

    private static final List<String> SUPPORTED_MEDIA_TYPES =
            Arrays.asList("text/plain", "text/html", "application/xhtml+xml");

    private Throwable error;

    ErrorResource(Throwable error) {
        this.error = error;
    }

    @Override
    public Method[] getSupportedMethods() {
        return Method.values();
    }

    @Override
    public void doGET() throws Exception {
        final Status status = toStatus(error);
        final Map<String,Object> templateVars = getCommonTemplateVars();
        templateVars.put("pageTitle", status.toString());
        templateVars.put("message", error.getMessage());

        Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false)) {
            templateVars.put("stackTrace", getStackTrace());
        }

        // Negotiate a response representation content type.
        // Web browsers will usually request `text/html` and
        // `application/xhtml+xml` in order of priority. In the absence
        // of either of those, we will prefer to return `text/plain`.
        String requestedType = negotiateContentType(SUPPORTED_MEDIA_TYPES);
        if (requestedType == null) {
            requestedType = "text/plain";
        }

        // Use a template that best fits the representation's content type.
        String template, mediaType;
        if (Arrays.asList("text/html", "application/xhtml+xml").
                contains(requestedType)) {
            template = "/error.html.vm";
            mediaType = "text/html";
        } else {
            template = "/error.txt.vm";
            mediaType = "text/plain";
        }

        getResponse().setStatus(status.getCode());
        getResponse().setHeader("Cache-Control", "no-cache, must-revalidate");
        getResponse().setHeader("Content-Type", mediaType + ";charset=UTF-8");

        new VelocityRepresentation(template, templateVars)
                .write(getResponse().getOutputStream());
    }

    private String getStackTrace() {
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            error.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            LOGGER.error("getStackTrace(): {}", e.getMessage(), e);
            return "Stack trace unavailable";
        }
    }

    private Status toStatus(Throwable t) {
        Status status;

        if (t instanceof ResourceException) {
            status = ((ResourceException) t).getStatus();
        } else if (t instanceof IllegalClientArgumentException ||
        } else if (t instanceof ValidationException ||
                t instanceof IllegalClientArgumentException ||
                t instanceof UnsupportedEncodingException) {
            status = Status.BAD_REQUEST;
        } else if (t instanceof UnsupportedOutputFormatException) {
            status = Status.UNSUPPORTED_MEDIA_TYPE;
        } else if (t instanceof FileNotFoundException ||
                t instanceof NoSuchFileException) {
            status = Status.NOT_FOUND;
        } else if (t instanceof UnsupportedSourceFormatException) {
            status = Status.NOT_IMPLEMENTED;
        } else {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        return status;
    }

}
