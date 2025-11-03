package edu.illinois.library.cantaloupe.controller.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot test for IIIF v2 Identifier Controller.
 * Tests the identifier redirect functionality and IIIF compliance.
 */
@WebMvcTest(IdentifierController.class)
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class IdentifierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");
        // Default: endpoint is enabled
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);
    }

    @Test
    void testRedirectToInfo_BasicIdentifier() throws Exception {
        String identifier = "test-image";

        mockMvc.perform(get("/iiif/2/{identifier}", identifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", containsString("/iiif/2/" + identifier + "/info.json")));
    }

    @Test
    void testRedirectToInfo_SpecialCharacters() throws Exception {
        String identifier = "test-image%20with%20spaces";

        mockMvc.perform(get("/iiif/2/{identifier}", identifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", is("/iiif/2/" + identifier + "/info.json")));
    }

    @Test
    void testRedirectToInfo_ComplexIdentifier() throws Exception {
        String identifier = "collection%2Fsubcollection%2Fimage.jpg";

        mockMvc.perform(get("/iiif/2/{identifier}", identifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", is("/iiif/2/" + identifier + "/info.json")));
    }

    @Test
    void testRedirectToInfo_WithContextPath() throws Exception {
        String identifier = "context-image";

        mockMvc.perform(get("/cantaloupe/iiif/2/{identifier}", identifier)
                .contextPath("/cantaloupe"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", is("/cantaloupe/iiif/2/" + identifier + "/info.json")));
    }

    @Test
    void testRedirectToInfo_EndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/2/test-image"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("This endpoint is disabled")));
    }

    @Test
    void testOptionsIdentifier() throws Exception {
        mockMvc.perform(options("/iiif/2/test-image"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"));
    }

    @Test
    void testOptionsIdentifier_EndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/2/test-image"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRedirectToInfo_LongIdentifier() throws Exception {
        String longIdentifier = "a".repeat(200);

        mockMvc.perform(get("/iiif/2/{identifier}", longIdentifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", containsString("/iiif/2/" + longIdentifier + "/info.json")));
    }

    @Test
    void testRedirectToInfo_EmptyIdentifier() throws Exception {
        // This should technically be caught by routing, but test anyway
        mockMvc.perform(get("/iiif/2/"))
                .andExpect(status().isNotFound()); // Spring Boot should return 404 for missing path variable
    }

    @Test
    void testRedirectToInfo_MultipleSlashesInIdentifier() throws Exception {
        String identifier = "path%2Fto%2Fmy%2Fimage";

        mockMvc.perform(get("/iiif/2/{identifier}", identifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", containsString(identifier)));
    }

    @Test
    void testRedirectToInfo_UnicodeIdentifier() throws Exception {
        String identifier = "测试图像"; // Chinese characters

        mockMvc.perform(get("/iiif/2/{identifier}", identifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", containsString("/info.json")));
    }
}
