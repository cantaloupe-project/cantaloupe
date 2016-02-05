package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class whose instances are intended to be serialized to JSON for use in IIIF
 * Image Information responses.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0: Image Information</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "@context", "@id", "protocol", "width", "height", "sizes",
        "tiles", "profile", "service" })
@JsonInclude(JsonInclude.Include.NON_NULL)
class ImageInfo {

    @JsonPropertyOrder({ "width", "height" })
    public static final class Size {
        public Integer height;
        public Integer width;

        /** No-op constructor needed by Jackson */
        public Size() {}

        public Size(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }
    }

    @JsonPropertyOrder({ "width", "height" })
    public static final class Tile {
        public Integer height;
        public List<Integer> scaleFactors = new ArrayList<>();
        public Integer width;

        /** No-op constructor needed by Jackson */
        public Tile() {}

        public Tile(Integer width, Integer height, List<Integer> scaleFactors) {
            this.width = width;
            this.height = height;
            this.scaleFactors = scaleFactors;
        }
    }

    @JsonProperty("@context")
    public final String context = "http://iiif.io/api/image/2/context.json";

    /**
     * "The height of the image to be requested."
     */
    public Integer height;

    @JsonProperty("@id")
    public String id;

    /**
     * "An array of profiles, indicated by either a URI or an object
     * describing the features supported. The first entry in the array must be
     * a compliance level URI, as defined below."
     */
    public final List<Object> profile = new ArrayList<>();

    public final String protocol = "http://iiif.io/api/image";

    /**
     * @see <a href="http://iiif.io/api/annex/services/">Linking to External
     * Services</a>
     */
    public Map service;

    /**
     * "A set of height and width pairs the client should use in the size
     * parameter to request complete images at different sizes that the server
     * has available. This may be used to let a client know the sizes that are
     * available when the server does not support requests for arbitrary sizes,
     * or simply as a hint that requesting an image of this size may result in
     * a faster response. A request constructed with the w,h syntax using these
     * sizes must be supported by the server, even if arbitrary width and
     * height are not."
     */
    public final List<Size> sizes = new ArrayList<>();

    /**
     * "A set of descriptions of the parameters to use to request regions of
     * the image (tiles) that are efficient for the server to deliver. Each
     * description gives a width, optionally a height for non-square tiles,
     * and a set of scale factors at which tiles of those dimensions are
     * available.
     */
    public final List<Tile> tiles = new ArrayList<>();

    /**
     * "The width of the image to be requested."
     */
    public Integer width;

}
