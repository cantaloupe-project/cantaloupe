package edu.illinois.library.cantaloupe.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.BasicAuth;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationProvider;
import edu.illinois.library.cantaloupe.config.FileConfiguration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for API configuration endpoints.
 * Replaces the previous api.ConfigurationResource class.
 */
@RestController
@RequestMapping("/configuration")
public class ConfigurationController {

    static final String BASIC_REALM = Application.getName() + " Control Panel";

    private final Configuration configuration;

    @Autowired
    public ConfigurationController(Configuration configuration ) {
        this.configuration = configuration;
    }

    /**
     * Returns JSON application configuration.
     * <strong>This may contain sensitive info and must be protected.</strong>
     * @throws ResourceException 
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfiguration(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        // Perform HTTP Basic Authentication
       BasicAuth.authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = configuration.getString(Key.ADMIN_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return configuration.getString(Key.ADMIN_SECRET);
            }
            return null;
        }, request, response);

        response.setHeader("Content-Type", "application/json;charset=UTF-8");

        Map<String, Object> map = Collections.emptyMap();
        final ConfigurationProvider provider = (ConfigurationProvider) configuration;
        final List<Configuration> wrappedConfigs = provider.getWrappedConfigurations();

        for (Configuration config : wrappedConfigs) {
            if (config instanceof FileConfiguration) {
                map = ((FileConfiguration) config).toMap();
                break;
            }
        }

        return ResponseEntity.ok(map);
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     * @throws ResourceException 
     */
    @PutMapping
    public ResponseEntity<Void> updateConfiguration(@RequestBody Map<String, Object> submittedConfig,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response)
            throws IOException, ResourceException {
        // Perform HTTP Basic Authentication
        BasicAuth.authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = configuration.getString(Key.ADMIN_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return configuration.getString(Key.ADMIN_SECRET);
            }
            return null;
        }, request, response);

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        submittedConfig.forEach((key, value) ->
                configuration.setProperty(key, value));

        configuration.save();

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        // Perform HTTP Basic Authentication
        BasicAuth.authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = configuration.getString(Key.ADMIN_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return configuration.getString(Key.ADMIN_SECRET);
            }
            return null;
        }, request, response);
        
        return ResponseEntity.noContent()
                .header("Allow", "GET,PUT,OPTIONS")
                .build();
    }
}
