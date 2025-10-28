package edu.illinois.library.cantaloupe.controller.iiif.v2;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

@WebMvcTest(LandingController.class)
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
public class LandingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Default: endpoint is enabled
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);
    }


    @Test
    void testIiif2Landing_WithEndpointEnabled() throws Exception {
        mockMvc.perform(get("/iiif/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("<h1>IIIF Image API 2.x Endpoint</h1>")));
    }

    @Test
    void testIiif2Landing_WithEndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);
        mockMvc.perform(get("/iiif/2"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("This endpoint is disabled")));
    }

    @Test
    void testRedirectToBase() throws Exception {
        mockMvc.perform(get("/iiif/2/"))
           .andExpect(redirectedUrl("/iiif/2"));
    }

    @Test
    void testOPTIONSWhenEnabled() throws Exception {
        mockMvc.perform(options("/iiif/2"))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Allow", "GET,OPTIONS"));
    }

    @Test
    void testOPTIONSWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/2"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("This endpoint is disabled")));
    }
}
