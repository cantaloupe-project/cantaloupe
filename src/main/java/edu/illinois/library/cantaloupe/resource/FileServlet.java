package edu.illinois.library.cantaloupe.resource;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
            final Path resPath = Paths.get(URLDecoder.decode(resURL.getPath()));
            try (InputStream is = new BufferedInputStream(resURL.openStream())) {
                response.setStatus(200);
                response.setHeader("Last-Modified", toRFC1123(Files.getLastModifiedTime(resPath)));
                response.setHeader("Cache-Control", "public, max-age=2592000");
                response.setHeader("Content-Length", "" + Files.size(resPath));
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

    private static String toRFC1123(FileTime fileTime) {
        OffsetDateTime odt = fileTime.toInstant().atOffset(ZoneOffset.UTC);
        return odt.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

}
