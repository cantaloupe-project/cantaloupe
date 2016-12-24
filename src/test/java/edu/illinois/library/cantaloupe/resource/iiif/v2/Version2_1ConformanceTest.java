package edu.illinois.library.cantaloupe.resource.iiif.v2;

import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

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
     *
     * @throws IOException
     */
    @Test
    public void testSquareRegion() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.2
     *
     * @throws IOException
     */
    @Test
    public void testMaxSize() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/max/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

}
