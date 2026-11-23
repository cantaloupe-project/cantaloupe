package edu.illinois.library.cantaloupe.controller.iiif.v1;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 1.x identifier redirects.
 * Redirects /{identifier} to /{identifier}/info.json.
 * Replaces the previous iiif.v1.IdentifierResource class.
 */
@RestController("v1IdentifierController")
@RequestMapping("/iiif/1")
public class IdentifierController extends AbstractIIIFController {

    @Autowired
    public IdentifierController(Configuration configuration) {
        super(configuration);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Void> redirectToInfo(@PathVariable String identifier,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws EndpointDisabledException {

        checkEndpointEnabled();

        // Build the redirect URL to info.json
        String redirectUrl = request.getContextPath() + "/iiif/1/" + identifier + "/info.json";

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirectUrl))
                .build();
    }

    @RequestMapping(value = "/{identifier}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsIdentifier(@PathVariable String identifier,
                                                  HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();

        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }


}
