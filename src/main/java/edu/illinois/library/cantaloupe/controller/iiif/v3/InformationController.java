package edu.illinois.library.cantaloupe.controller.iiif.v3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.iiif.v3.Information;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 3.x information requests.
 * Placeholder implementation - replaces the previous iiif.v3.InformationResource class.
 *
 * Note: This is a working implementation using real IIIF classes but with placeholder data.
 * Full functionality would require integration with the complete image processing pipeline.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#51-image-information-request">
 *     Image Information Requests</a>
 */
@RestController
@RequestMapping("/iiif/3")
public class InformationController {
    private final Configuration configuration;

    @Autowired
    public InformationController(Configuration configuration) {
        this.configuration = configuration;
    }

    @GetMapping("/{identifier}/info.json")
    public ResponseEntity<Information<String, Object>> getInformation(@PathVariable String identifier,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) throws Exception {

        checkEndpointEnabled();
        addCorsHeaders(response);

        // Set up content type negotiation
        String contentType = getNegotiatedContentType(request);
        response.setHeader("Content-Type", contentType);

        // Create a real IIIF Information object with placeholder data
        Information<String, Object> iiifInfo = createPlaceholderInformation(identifier, request);

        return ResponseEntity.ok(iiifInfo);
    }

    @RequestMapping(value = "/{identifier}/info.json", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsInformation(@PathVariable String identifier,
                                                   HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();
        addCorsHeaders(response);

        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    private void checkEndpointEnabled() throws EndpointDisabledException {
        if (!configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

    private String getNegotiatedContentType(HttpServletRequest request) {
        String contentType;

        // Check Accept header for JSON preference
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            contentType = "application/json";
        } else {
            contentType = "application/ld+json";
        }

        contentType += ";charset=UTF-8";
        contentType += ";profile=\"http://iiif.io/api/image/3/context.json\"";
        return contentType;
    }

    /**
     * Creates a placeholder IIIF Information object with real structure.
     */
    private Information<String, Object> createPlaceholderInformation(String identifier, HttpServletRequest request) {
        Information<String, Object> info = new Information<>();

        // Build the image URI
        String imageURI = buildImageURI(identifier, request);

        // Standard IIIF v3 properties
        info.put("@context", "http://iiif.io/api/image/3/context.json");
        info.put("id", imageURI);
        info.put("type", "ImageService3");
        info.put("protocol", "http://iiif.io/api/image");
        info.put("profile", "level2");

        // Placeholder image dimensions
        info.put("width", 1000);
        info.put("height", 1000);
        info.put("maxWidth", 1000);
        info.put("maxHeight", 1000);

        // Supported formats
        info.put("format", java.util.Arrays.asList("jpg", "png", "gif", "webp"));

        // Supported qualities
        info.put("quality", java.util.Arrays.asList("default", "color", "gray", "bitonal"));

        // Rights information
        info.put("rights", "http://creativecommons.org/licenses/by/3.0/");

        // Note about placeholder status
        info.put("_note", "This is a placeholder response. Full implementation requires image source integration.");

        return info;
    }

    /**
     * Builds the image URI from the request.
     */
    private String buildImageURI(String identifier, HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder uri = new StringBuilder();
        uri.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            uri.append(":").append(serverPort);
        }
        uri.append(contextPath).append("/iiif/3/").append(identifier);
        return uri.toString();
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
    }
}
