package edu.illinois.library.cantaloupe.controller;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LandingController.class)
public class LandingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @Test
    void testGetLanding() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(header().string("Cache-Control", "public, max-age=2147483647"))
                .andExpect(content().string(containsString("<body")));
    }

    @Test
    void testOptions() throws Exception {
        mockMvc.perform(options("/"))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Allow", "GET,OPTIONS"));
    }
}
