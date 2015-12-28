package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Class whose instances are intended to be serialized to JSON for use in IIIF
 * Image Information responses.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0</a>
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

    @JsonProperty("@context")
    public final String context = "http://iiif.io/api/image/2/context.json";

    public Integer height;

    @JsonProperty("@id")
    public String id;

    public final List<Object> profile = new ArrayList<>();

    public final String protocol = "http://iiif.io/api/image";

    public final List<Size> sizes = new ArrayList<>();

    public Integer width;

}
