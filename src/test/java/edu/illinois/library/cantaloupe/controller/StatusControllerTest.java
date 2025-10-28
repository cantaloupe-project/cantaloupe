package edu.illinois.library.cantaloupe.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;

@WebMvcTest(StatusController.class)
public class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(true);


        // Set up basic auth credentials for tests
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("admin");
        when(configuration.getString(Key.ADMIN_SECRET)).thenReturn("secret");
    }

    @Test
    void testGETWithNoCredentials() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithInvalidCredentials() throws Exception {
        String invalidAuth = Base64.getEncoder().encodeToString("invalid:invalid".getBytes());
        mockMvc.perform(get("/status")
                .header("Authorization", "Basic " + invalidAuth))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(get("/status")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(content().string(containsString("\"infoCache\":")));
    }

    @Test
    void testOPTIONSWithNoCredentials() throws Exception {
        mockMvc.perform(options("/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testOPTIONSWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(options("/status")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"));
    }

    @Test
    void testGETWithNoConfiguredCredentials() throws Exception {
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("");
        when(configuration.getString(Key.ADMIN_SECRET, "")).thenReturn("");

        mockMvc.perform(get("/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }


    @Test
    void testGETWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(false);

        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(get("/status")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isForbidden());
    }
}
