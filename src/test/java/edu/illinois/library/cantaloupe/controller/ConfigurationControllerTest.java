package edu.illinois.library.cantaloupe.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.ConfigurationProvider;
import edu.illinois.library.cantaloupe.config.Key;

@WebMvcTest(ConfigurationController.class)
public class ConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigurationProvider configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Set up basic auth credentials for tests
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("admin");
        when(configuration.getString(Key.ADMIN_SECRET)).thenReturn("secret");
    }

    @Test
    void testGETWithNoCredentials() throws Exception {
        mockMvc.perform(get("/configuration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithInvalidCredentials() throws Exception {
        String invalidAuth = Base64.getEncoder().encodeToString("invalid:invalid".getBytes());
        mockMvc.perform(get("/configuration")
                .header("Authorization", "Basic " + invalidAuth))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(get("/configuration")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
    }

    @Test
    void testPUTWithNoCredentials() throws Exception {
        mockMvc.perform(put("/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testPUTWithInvalidCredentials() throws Exception {
        String invalidAuth = Base64.getEncoder().encodeToString("invalid:invalid".getBytes());
        mockMvc.perform(put("/configuration")
                .header("Authorization", "Basic " + invalidAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testPUTWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(put("/configuration")
                .header("Authorization", "Basic " + validAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testOPTIONSWithNoCredentials() throws Exception {
        mockMvc.perform(options("/configuration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testOPTIONSWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(options("/configuration")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,PUT,OPTIONS"));
    }

    @Test
    void testGETWithNoConfiguredCredentials() throws Exception {
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("");
        when(configuration.getString(Key.ADMIN_SECRET, "")).thenReturn("");

        mockMvc.perform(get("/configuration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    // @Test
    // void testGETWhenDisabled() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.ADMIN_ENABLED, false);
    //     try {
    //         client.send();
    //         fail("Expected exception");
    //     } catch (ResourceException e) {
    //         assertEquals(403, e.getStatusCode());
    //     }
    // }

    // @Test
    // void testGETResponseHeaders() throws Exception {
    //     Response response = client.send();
    //     Headers headers = response.getHeaders();
    //     assertEquals(6, headers.size());

    //     // Cache-Control
    //     assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
    //     // Content-Length
    //     assertNotNull(headers.getFirstValue("Content-Length"));
    //     // Content-Type
    //     assertTrue("application/json;charset=UTF-8".equalsIgnoreCase(
    //             headers.getFirstValue("Content-Type")));
    //     // Date
    //     assertNotNull(headers.getFirstValue("Date"));
    //     // Server
    //     assertNotNull(headers.getFirstValue("Server"));
    //     // X-Powered-By
    //     assertEquals(Application.getName() + "/" + Application.getVersion(),
    //             headers.getFirstValue("X-Powered-By"));
    // }

    // @Test
    // void testGETResponseBody() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty("test", "cats");

    //     Response response = client.send();
    //     assertTrue(response.getBodyAsString().contains("\"test\":\"cats\""));
    // }

    // @Override
    // @Test
    // public void testOPTIONSWhenEnabled() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.ADMIN_ENABLED, true);

    //     client.setMethod(Method.OPTIONS);
    //     Response response = client.send();
    //     assertEquals(204, response.getStatus());

    //     Headers headers = response.getHeaders();
    //     List<String> methods =
    //             List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
    //     assertEquals(3, methods.size());
    //     assertTrue(methods.contains("GET"));
    //     assertTrue(methods.contains("PUT"));
    //     assertTrue(methods.contains("OPTIONS"));
    // }

    // @Test
    // void testPUTWhenEnabled() throws Exception {
    //     Map<String,Object> entityMap = new HashMap<>();
    //     entityMap.put("test", "cats");
    //     String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);

    //     client.setMethod(Method.PUT);
    //     client.setEntity(entityStr);
    //     client.setContentType(new MediaType("application/json"));
    //     client.send();

    //     assertEquals("cats", Configuration.getInstance().getString("test"));
    // }

    // @Test
    // void testPUTWhenDisabled() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.ADMIN_ENABLED, false);

    //     Map<String,Object> entityMap = new HashMap<>();
    //     entityMap.put("test", "cats");
    //     String entityStr = new ObjectMapper().writer().
    //             writeValueAsString(entityMap);

    //     client.setMethod(Method.PUT);
    //     client.setEntity(entityStr);
    //     client.setContentType(new MediaType("application/json"));

    //     try {
    //         client.send();
    //         fail("Expected exception");
    //     } catch (ResourceException e) {
    //         assertEquals(403, e.getStatusCode());
    //     }
    // }
}
