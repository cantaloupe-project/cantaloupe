package edu.illinois.library.cantaloupe.resource;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles requests for static files.
 */
public class FileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        String path = getContextRelativePath(
                request.getRequestURI(), request.getContextPath());
        path = path.replaceAll("^\\/static", "/webapp");

        response.setHeader("Cache-Control", "public, max-age=2592000");
        try (InputStream is =
                     new BufferedInputStream(getClass().getResourceAsStream(path))) {
            is.transferTo(response.getOutputStream());
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

}
