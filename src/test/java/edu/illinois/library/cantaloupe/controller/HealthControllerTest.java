package edu.illinois.library.cantaloupe.controller;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Clear any previous health overrides
        HealthChecker.getSourceUsages().clear();
        HealthChecker.overrideHealth(null);

        // Default: health endpoint is enabled
        when(configuration.getBoolean(Key.HEALTH_ENDPOINT_ENABLED, false)).thenReturn(true);
    }

    @Test
    void testGETWithEndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.HEALTH_ENDPOINT_ENABLED, false)).thenReturn(false);

        mockMvc.perform(get("/health"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETWithGreenStatus() throws Exception {
        when(configuration.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)).thenReturn(false);

        // Note: In the original test, an actual image request was made to exercise the pipeline.
        // In this MockMvc version, we're just testing the controller behavior directly.
        // The health status should be GREEN by default.

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.color").value("GREEN"));
    }

    @Test
    void testGETWithYellowStatus() throws Exception {
        when(configuration.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)).thenReturn(true);

        Health health = new Health();
        health.setMinColor(Health.Color.YELLOW);
        HealthChecker.overrideHealth(health);

        mockMvc.perform(get("/health"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGETWithRedStatus() throws Exception {
        when(configuration.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)).thenReturn(true);

        Health health = new Health();
        health.setMinColor(Health.Color.RED);
        HealthChecker.overrideHealth(health);

        mockMvc.perform(get("/health"))
                .andExpect(status().isInternalServerError());
    }

}
