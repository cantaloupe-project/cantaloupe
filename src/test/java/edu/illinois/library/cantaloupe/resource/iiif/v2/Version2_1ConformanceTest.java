package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.http.Response;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.1 spec. Methods
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
    public void testSquareRegion() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/default.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.2
     */
    @Test
    public void testMaxSize() throws Exception {
        client = newClient("/" + IMAGE + "/full/max/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

}
