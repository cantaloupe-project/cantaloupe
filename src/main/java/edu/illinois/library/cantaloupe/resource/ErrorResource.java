package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.operation.IllegalScaleException;
import edu.illinois.library.cantaloupe.operation.IllegalSizeException;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.OutputFormatException;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.resource.iiif.FormatException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates a {@link Throwable} to an HTTP 4xx or 5xx-level response.
 */
class ErrorResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ErrorResource.class);

    private final Throwable error;
    private HttpServletRequest request;
    private HttpServletResponse response;

    private static Status toStatus(Throwable t) {
        Status status;
        if (t instanceof ResourceException) {
            status = ((ResourceException) t).getStatus();
        } else if (t instanceof IllegalSizeException ||
                t instanceof IllegalScaleException ||
                t instanceof AccessDeniedException) {
            status = Status.FORBIDDEN;
        } else if (t instanceof ValidationException ||
                t instanceof IllegalClientArgumentException ||
                t instanceof UnsupportedEncodingException) {
            status = Status.BAD_REQUEST;
        } else if (t instanceof FormatException ||
                t instanceof OutputFormatException) {
            status = Status.UNSUPPORTED_MEDIA_TYPE;
        } else if (t instanceof FileNotFoundException ||
                t instanceof NoSuchFileException) {
            status = Status.NOT_FOUND;
        } else if (t instanceof SourceFormatException) {
            status = Status.NOT_IMPLEMENTED;
        } else {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        return status;
    }


    ErrorResource(Throwable error, HttpServletRequest request, HttpServletResponse response) {
        this.error = error;
        this.request = request;
        this.response = response;
    }

    public void doGET() throws Exception {
        final Status status = toStatus(error);
        log(status.getCode());

        final Map<String,Object> templateVars = new HashMap<>();
        templateVars.put("baseUri", request.getContextPath());
        templateVars.put("pageTitle", status.toString());
        templateVars.put("message", error.getMessage());

        Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false)) {
            templateVars.put("stackTrace", getStackTrace());
        }

        // Use a template that best fits the representation's content type.
        String template, mediaType;
        String header = request.getHeader("Accept");
        if (header == null || header.contains("html")) {
            template = "/error.html.vm";
            mediaType = "text/html";
        } else {
            template = "/error.txt.vm";
            mediaType = "text/plain";
        }

        response.setStatus(status.getCode());
        // Only show the x-powered-by header if configured to do so.
        if (config.getBoolean(Key.HEADERS_POWERED_BY_DISPLAY, true)) {
          response.setHeader("X-Powered-By",
                  Application.getName() + "/" + Application.getVersion());
        }
        response.setHeader("Cache-Control", "no-cache, must-revalidate");
        response.setHeader("Content-Type", mediaType + ";charset=UTF-8");

        new VelocityRepresentation(template, templateVars)
                .write(response.getOutputStream());
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

    private void log(int statusCode) {
        if (!Configuration.getInstance().getBoolean(Key.LOG_ERROR_RESPONSES, false)) {
            return;
        }
        String message = "Responding with HTTP {} to {} {}: {}";
        Object[] args = {
                statusCode,
                request.getMethod(),
                request.getRequestURI(),
                error.getMessage(),
                error };
        if (statusCode >= 500) {
            LOGGER.error(message, args);
        } else if (statusCode != 404) {
            LOGGER.warn(message, args);
        }
    }

}
