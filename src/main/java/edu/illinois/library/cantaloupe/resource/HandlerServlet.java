package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Front-controller Servlet that handles all requests.
 */
public class HandlerServlet extends HttpServlet {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HandlerServlet.class);

    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response) {
        handle(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {
        handle(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request,
                          HttpServletResponse response) {
        handle(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request,
                             HttpServletResponse response) {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) {
        handle(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response) {
        handle(request, response);
    }

    private void handle(HttpServletRequest request,
                        HttpServletResponse response) {
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
            resource.setPathArguments(route.getPathArguments());
            resource.setRequest(new Request(request));
            resource.setResponse(response);
            resource.doInit();

            final List<Method> supportedMethods =
                    Arrays.asList(resource.getSupportedMethods());
            // If the request method is HEAD and GET is supported
            if (("HEAD".equals(request.getMethod()) && supportedMethods.contains(Method.GET)) ||
                    // or if the request method is OPTIONS
                    "OPTIONS".equals(request.getMethod()) ||
                    // or if the request method is supported
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
                    request.getMethod(), request.getPathInfo(),
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
        ErrorResource resource = new ErrorResource(t);
        try {
            response.setContentType("text/html;charset=UTF-8");
            resource.setRequest(new Request(request));
            resource.setResponse(response);
            resource.doInit();
            resource.doGET();
        } catch (Throwable t2) {
            // Fall back to a plain text stack trace.
            response.setContentType("text/plain;charset=UTF-8");
            try {
                PrintWriter writer = response.getWriter();
                writer.println("Unrecoverable error in " +
                        HandlerServlet.class.getSimpleName() + ":");
                writer.println("");
                t2.printStackTrace(writer);
            } catch (IllegalStateException e) {
                if ("STREAM".equals(e.getMessage())) {
                    // This means that something was writing to the response
                    // OutputStream but was interrupted, probably by the user
                    // terminating the request, and trying to acquire a writer
                    // above threw an exception because you aren't allowed to
                    // use a writer after you've written to the output stream.
                    // Anyway, no big deal, we'll just log it.
                    LOGGER.debug("Failed to acquire an error writer after " +
                            "failing to fully write the response. Most " +
                            "likely this was caused by the client closing " +
                            "the connection and is not a problem.");
                }
            } catch (IOException e) {
                LOGGER.error("handleError(): {}", e.getMessage(), e);
            }
        } finally {
            resource.destroy();
        }
    }

}
