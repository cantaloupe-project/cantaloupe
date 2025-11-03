package edu.illinois.library.cantaloupe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.ConfigurationProvider;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.MapConfiguration;
import edu.illinois.library.cantaloupe.controller.admin.AdminConfigurationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminConfigurationController.class)
public class AdminConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigurationProvider configuration;

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
        mockMvc.perform(get("/admin/configuration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithInvalidCredentials() throws Exception {
        String invalidAuth = Base64.getEncoder().encodeToString("invalid:invalid".getBytes());
        mockMvc.perform(get("/admin/configuration")
                .header("Authorization", "Basic " + invalidAuth))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        Configuration config = new MapConfiguration();
        config.setProperty("test", "cats");
        List<Configuration> configs = new ArrayList<Configuration>() {{ add(config); }};
        when(configuration.getWrappedConfigurations()).thenReturn(configs);

        mockMvc.perform(get("/admin/configuration")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(content().string(containsString("\"test\":\"cats\"")));
    }

    @Test
    void testGETWithNoConfiguredCredentials() throws Exception {
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("");
        when(configuration.getString(Key.ADMIN_SECRET, "")).thenReturn("");

        mockMvc.perform(get("/admin/configuration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(false);

        mockMvc.perform(get("/admin/configuration"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPUTWithNoCredentials() throws Exception {
        mockMvc.perform(put("/admin/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testPUTWithInvalidCredentials() throws Exception {
        String invalidAuth = Base64.getEncoder().encodeToString("invalid:invalid".getBytes());
        mockMvc.perform(put("/admin/configuration")
                .header("Authorization", "Basic " + invalidAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testPUTWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);

        mockMvc.perform(put("/admin/configuration")
                .header("Authorization", "Basic " + validAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(entityStr))
                .andExpect(status().isNoContent());
        verify(configuration, times(1)).setProperty("test", "cats");
    }

    @Test
    void testPUTWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(false);
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);

          mockMvc.perform(put("/admin/configuration")
                .header("Authorization", "Basic " + validAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(entityStr))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOPTIONSWithNoCredentials() throws Exception {
        mockMvc.perform(options("/admin/configuration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testOPTIONSWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(options("/admin/configuration")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,PUT,OPTIONS"));
    }
}
