package edu.illinois.library.cantaloupe.controller.admin;

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

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationProvider;
import edu.illinois.library.cantaloupe.config.FileConfiguration;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for admin configuration endpoints.
 * Replaces the previous admin.ConfigurationResource class.
 */
@RestController
@RequestMapping("/admin/configuration")
public class AdminConfigurationController {
    private final Configuration configuration;

    @Autowired
    public AdminConfigurationController(Configuration configuration ) {
        this.configuration = configuration;
    }

    /**
     * Returns JSON application configuration for the admin interface.
     * <strong>This may contain sensitive info and must be protected.</strong>
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfiguration(HttpServletResponse response) {
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
     */
    @PutMapping
    public ResponseEntity<Void> updateConfiguration(@RequestBody Map<String, Object> submittedConfig)
            throws IOException {

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        submittedConfig.forEach((key, value) ->
                configuration.setProperty(key, value));

        configuration.save();

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent()
                .header("Allow", "GET,PUT,OPTIONS")
                .build();
    }
}
