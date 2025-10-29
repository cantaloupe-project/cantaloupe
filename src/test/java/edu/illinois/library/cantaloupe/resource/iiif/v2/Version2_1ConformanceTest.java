package edu.illinois.library.cantaloupe.resource.iiif.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.1 spec using MockMvc. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.1/#image-information">IIIF Image
 * API 2.1</a>
 */
public class Version2_1ConformanceTest extends Version2_0ConformanceTest {

    /**
     * 4.1
     */
    @Test
    void testSquareRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/square/full/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(56, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.2
     */
    @Test
    void testMaxSize() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/max/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

}
