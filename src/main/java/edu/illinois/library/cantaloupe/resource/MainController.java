package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.*;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Main Spring Controller that handles all requests, replacing the HandlerServlet.
 * This controller uses the existing Route and AbstractResource system to maintain
 * compatibility with the current Cantaloupe architecture.
 */
@Controller
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    /**
     * Handle all requests using a catch-all mapping.
     * This allows us to use the existing Route system for path matching.
     */
    @RequestMapping("**")
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        final Stopwatch requestClock = new Stopwatch();

        final String path = getContextRelativePath(
                request.getRequestURI(), request.getContextPath());

        AbstractResource resource = null;

        try {
            Route route = Route.forPath(path);
            if (route == null) {
                throw new ResourceException(Status.NOT_FOUND,
                        "No route for path: " + path);
            }

            resource = route.getResource().getDeclaredConstructor().newInstance();
            resource.setRequest(new Request(request, route.getPathArguments()));
            resource.setResponse(response);
            resource.doInit();

            final List<Method> supportedMethods =
                    List.of(resource.getSupportedMethods());

            // Check if the request method is supported
            if (("HEAD".equals(request.getMethod()) && supportedMethods.contains(Method.GET)) ||
                    "OPTIONS".equals(request.getMethod()) ||
                    supportedMethods.contains(Method.valueOf(request.getMethod()))) {

                switch (request.getMethod()) {
                    case "DELETE":
                        resource.doDELETE();
                        break;
                    case "GET":
                        resource.doGET();
                        break;
                    case "HEAD":
                        resource.doHEAD();
                        break;
                    case "OPTIONS":
                        resource.doOPTIONS();
                        break;
                    case "POST":
                        resource.doPOST();
                        break;
                    case "PUT":
                        resource.doPUT();
                        break;
                    default:
                        throw new ResourceException(Status.METHOD_NOT_ALLOWED);
                }
            } else {
                throw new ResourceException(Status.METHOD_NOT_ALLOWED);
            }
        } catch (Throwable t) {
            handleError(request, response, t);
        } finally {
            if (resource != null) {
                resource.destroy();
            }
            LOGGER.debug("Responded to {} {} with HTTP {} in {}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), requestClock);
        }
    }

    /**
     * @param fullPath    Full URI path including the context path.
     * @param contextPath Context path (path above the application root).
     * @return            Application root-relative path.
     */
    private String getContextRelativePath(String fullPath, String contextPath) {
        if (contextPath == null) {
            contextPath = "";
        }
        return fullPath.substring(contextPath.length());
    }

    private void handleError(HttpServletRequest request,
                             HttpServletResponse response,
                             Throwable t) {
        // Try to use an ErrorResource, which will render an HTML template.
        ErrorResource resource = new ErrorResource(t, request, response);
        try {
            // N.B.: the response status will be set by ErrorResource based on
            // the type of Throwable.
            resource.doGET();
        } catch (IllegalClientArgumentException e) {
            handleError(response, e, 400);
        } catch (Throwable t2) {
            handleError(response, t2, 500);
        }
    }

    private void handleError(HttpServletResponse response,
                             Throwable t,
                             int status) {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        try {
            PrintWriter writer = response.getWriter();
            writer.print("Unrecoverable error in " +
                    MainController.class.getSimpleName());
            if (isPrintingStackTraces()) {
                writer.println(":");
                writer.println("");
                t.printStackTrace(writer);
            }
        } catch (IllegalStateException e) {
            if ("STREAM".equals(e.getMessage())) {
                // This means that something was writing to the response
                // OutputStream but was interrupted, probably by the user
                // terminating the request, and trying to acquire a writer
                // above threw an exception because you aren't allowed to
                // use a writer after you've written to the output stream.
                LOGGER.debug("Failed to acquire an error writer after " +
                        "failing to fully write the response. Most " +
                        "likely this was caused by the client closing " +
                        "the connection and is not a problem.");
            }
        } catch (IOException e) {
            LOGGER.error("handleError(): {}", e.getMessage(), e);
        }
    }

    private boolean isPrintingStackTraces() {
        Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false);
    }
}
