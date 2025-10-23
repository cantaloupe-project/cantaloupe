package edu.illinois.library.cantaloupe.controller.iiif.v3;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.illinois.library.cantaloupe.config.Configuration;

@WebMvcTest(LandingController.class)
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
public class LandingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @Test
    void testIiif3Landing() throws Exception {
        mockMvc.perform(get("/iiif/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("<h1>IIIF Image API 3.x Endpoint</h1>")));
    }

}
