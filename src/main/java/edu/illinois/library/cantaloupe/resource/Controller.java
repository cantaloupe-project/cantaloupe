package edu.illinois.library.cantaloupe.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;

import java.util.Enumeration;
import java.util.Map;

public abstract class Controller {
    public Controller(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        logRequest();
        addDefaultHeaders();
    }



    protected HttpServletRequest request;
    protected HttpServletResponse response;

    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

    protected abstract void doGet(Request request) throws Exception;


    protected void addDefaultHeaders() {
        if (Configuration.getInstance().getBoolean(Key.HEADERS_POWERED_BY_DISPLAY, true)) {
          response.setHeader("X-Powered-By",
                  Application.getName() + "/" + Application.getVersion());
        }
    }

    protected void doOptions() throws EndpointDisabledException{
        if (!enabled()) {
            throw new EndpointDisabledException();
        }
        response.setHeader("Allow", "GET,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization");

        response.setStatus(204);
    }

    protected void renderHtml(String templatePath, Map<String,Object> templateVars ) throws Exception {
        response.setHeader("Content-Type", "text/html;charset=UTF-8");

        new VelocityRepresentation(templatePath, templateVars)
                .write(response.getOutputStream());
    }

    protected boolean enabled() {
        return true;
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    private void logRequest() {
        StringBuilder headersString = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if ("Authorization".equalsIgnoreCase(headerName)) {
                headersString.append(headerName).append(": ******\n");
                continue; // Skip logging the actual Authorization header value
            }
            Enumeration<String> headerValues = request.getHeaders(headerName);

            // Handle multiple values for a single header name
            headersString.append(headerName).append(": ");
            boolean firstValue = true;
            while (headerValues.hasMoreElements()) {
                if (!firstValue) {
                    headersString.append(", "); // Separate multiple values with a comma
                }
                headersString.append(headerValues.nextElement());
                firstValue = false;
            }
            headersString.append("\n"); // Newline after each header entry
        }
                // Log request info.
        getLogger().info("Handling {} {}",
                request.getMethod(), request.getServletPath());

        getLogger().debug("Request headers: {}", headersString.toString());
    }
}
