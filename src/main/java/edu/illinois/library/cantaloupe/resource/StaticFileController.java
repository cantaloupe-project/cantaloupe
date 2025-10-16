package edu.illinois.library.cantaloupe.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;


/**
 * Spring Controller that handles requests for static files, replacing the FileServlet.
 */
@Controller
@RequestMapping("/static")
public class StaticFileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticFileController.class);

    /**
     * Handles requests for static files under /static/** paths.
     * Maps requests from /static/* to /webapp/* in the classpath.
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> handleStaticFile(HttpServletRequest request) {
        String pathStr = request.getRequestURI();

        // Remove the /static prefix and replace with /webapp for classpath lookup
        pathStr = pathStr.replaceAll("^/static", "/webapp");

        try {
            ClassPathResource resource = new ClassPathResource(pathStr);

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type based on file extension
            MediaType contentType = getContentType(pathStr);

            // Set cache headers for static resources (30 days)
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("public, max-age=2592000");
            headers.setContentType(contentType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            LOGGER.error("Error serving static file: " + pathStr, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Determines the appropriate MediaType based on file extension.
     */
    private MediaType getContentType(String path) {
        int extIdx = path.lastIndexOf(".");
        if (extIdx > 0) {
            String ext = path.substring(extIdx).toLowerCase();

            switch (ext) {
                case ".css":
                    return MediaType.valueOf("text/css");
                case ".js":
                    return MediaType.valueOf("application/javascript");
                case ".png":
                    return MediaType.IMAGE_PNG;
                case ".svg":
                    return MediaType.valueOf("image/svg+xml");
                case ".jpg":
                case ".jpeg":
                    return MediaType.IMAGE_JPEG;
                case ".gif":
                    return MediaType.IMAGE_GIF;
                case ".html":
                    return MediaType.TEXT_HTML;
                case ".json":
                    return MediaType.APPLICATION_JSON;
                case ".xml":
                    return MediaType.APPLICATION_XML;
                case ".ico":
                    return MediaType.valueOf("image/x-icon");
                case ".woff":
                    return MediaType.valueOf("font/woff");
                case ".woff2":
                    return MediaType.valueOf("font/woff2");
                case ".ttf":
                    return MediaType.valueOf("font/ttf");
                case ".eot":
                    return MediaType.valueOf("application/vnd.ms-fontobject");
                default:
                    return MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
