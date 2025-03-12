package edu.illinois.library.cantaloupe.resource;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Handles requests for static files.
 */
public class FileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        String pathStr = getContextRelativePath(
                request.getRequestURI(), request.getContextPath());
        pathStr = pathStr.replaceAll("^\\/static", "/webapp");

        final URL resURL = getClass().getResource(pathStr);
        if (resURL != null) {
            try (InputStream is = new BufferedInputStream(resURL.openStream())) {
                response.setStatus(200);
                response.setHeader("Cache-Control", "public, max-age=2592000");
                response.setHeader("Content-Type", getContentType(pathStr));
                is.transferTo(response.getOutputStream());
            }
        } else {
            response.setStatus(404);
        }
    }

    private String getContentType(String path) {
        int extIdx = path.lastIndexOf(".");
        if (extIdx > 0) {
            String ext = path.substring(extIdx);
            // This statement must include a case for every static file type
            // served by the app.
            switch (ext) {
                case ".css":
                    return "text/css";
                case ".js":
                    return "application/javascript";
                case ".png":
                    return "image/png";
                case ".svg":
                    return "image/svg";
            }
        }
        return "application/octet-stream";
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
