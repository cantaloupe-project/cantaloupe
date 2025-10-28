package edu.illinois.library.cantaloupe.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.controller.admin.AdminController;
import edu.illinois.library.cantaloupe.image.MetaIdentifierTransformerFactory;
import edu.illinois.library.cantaloupe.source.SourceFactory;

@WebMvcTest(AdminController.class)
@Import({SourceFactory.class,
        MetaIdentifierTransformerFactory.class,
        })
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");
        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")).thenReturn("ManualSelectionStrategy");
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(true);

        // Set up basic auth credentials for tests
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("admin");
        when(configuration.getString(Key.ADMIN_SECRET)).thenReturn("secret");
    }

    private void stubCacheControlHeaders() {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(true);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)).thenReturn(true);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)).thenReturn(true);
        when(configuration.getString(Key.CLIENT_CACHE_MAX_AGE, "")).thenReturn("1234");
        when(configuration.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "")).thenReturn("4567");
    }

    @Test
    void testGETWithNoCredentials() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithInvalidCredentials() throws Exception {
        String invalidAuth = Base64.getEncoder().encodeToString("invalid:invalid".getBytes());
        mockMvc.perform(get("/admin")
                .header("Authorization", "Basic " + invalidAuth))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"" + Application.getName() + " Control Panel\" charset=\"UTF-8\""));
    }

    @Test
    void testGETWithValidCredentials() throws Exception {
        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(get("/admin")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWhenAdminDisabled() throws Exception {
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(false);

        String validAuth = Base64.getEncoder().encodeToString("admin:secret".getBytes());
        mockMvc.perform(get("/admin")
                .header("Authorization", "Basic " + validAuth))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETWithNoConfiguredCredentials() throws Exception {
        when(configuration.getString(Key.ADMIN_USERNAME, "")).thenReturn("");
        when(configuration.getString(Key.ADMIN_SECRET, "")).thenReturn("");

        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized());
    }

    // @Test
    // void testGETWhenEnabled() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.ADMIN_ENABLED, true);

    //     Response response = client.send();
    //     assertEquals(200, response.getStatus());
    // }

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
    //     // Content-Type
    //     assertTrue("text/html;charset=UTF-8".equalsIgnoreCase(
    //             headers.getFirstValue("Content-Type")));
    //     // Date
    //     assertNotNull(headers.getFirstValue("Date"));
    //     // Server
    //     assertNotNull(headers.getFirstValue("Server"));
    //     // Transfer-Encoding
    //     assertEquals("chunked", headers.getFirstValue("Transfer-Encoding"));
    //     // X-Powered-By
    //     assertEquals(Application.getName() + "/" + Application.getVersion(),
    //             headers.getFirstValue("X-Powered-By"));
    // }
}
