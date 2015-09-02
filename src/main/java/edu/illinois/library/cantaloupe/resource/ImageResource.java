package edu.illinois.library.cantaloupe.resource;

import java.util.Map;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Quality;
import edu.illinois.library.cantaloupe.image.Region;
import edu.illinois.library.cantaloupe.image.Rotation;
import edu.illinois.library.cantaloupe.image.Size;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Created by alexd on 9/1/15.
 */
public class ImageResource extends ServerResource {

    /**
     * Encapsulates the parameters of an IIIF request.
     *
     * @see <a href="http://iiif.io/api/image/2.0/#image-request-parameters">IIIF Image API 2.0</a>
     */
    private class Parameters {

        private Format format;
        private Quality quality;
        private Region region;
        private Rotation rotation;
        private Size size;

        /**
         * @param region
         * @param size
         * @param rotation
         * @param quality
         * @param format
         */
        public Parameters(String region, String size, String rotation,
                          String quality, String format) {
            this.format = Format.valueOf(format.toUpperCase());
            this.quality = Quality.valueOf(quality.toUpperCase());
            this.region = Region.fromUri(region);
            this.rotation = Rotation.fromUri(rotation);
            this.size = Size.fromUri(size);
        }

    }

    @Get
    public Representation doGet() {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");

        Parameters params = new Parameters(region, size, rotation, quality,
                format);

        return new StringRepresentation("ImageResource");
    }

}
