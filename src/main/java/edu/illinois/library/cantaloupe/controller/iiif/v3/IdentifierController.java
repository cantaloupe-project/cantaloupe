package edu.illinois.library.cantaloupe.controller.iiif.v3;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;

/**
 * Spring Boot controller for IIIF Image API 3.x identifier redirects.
 * Redirects /{identifier} to /{identifier}/info.json.
 * Replaces the previous iiif.v3.IdentifierResource class.
 */
@RestController
@RequestMapping("/iiif/3")
public class IdentifierController {

    @GetMapping("/{identifier}")
    public ResponseEntity<Void> redirectToInfo(@PathVariable String identifier,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws EndpointDisabledException {

        checkEndpointEnabled();

        // Build the redirect URL to info.json
        String redirectUrl = buildBaseUrl(request) + "/iiif/3/" + identifier + "/info.json";

        // Add CORS headers
        addCorsHeaders(response);

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirectUrl))
                .build();
    }

    @RequestMapping(value = "/{identifier}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsIdentifier(@PathVariable String identifier,
                                                  HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();
        addCorsHeaders(response);

        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    private void checkEndpointEnabled() throws EndpointDisabledException {
        if (!Configuration.getInstance().getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
    }
}
