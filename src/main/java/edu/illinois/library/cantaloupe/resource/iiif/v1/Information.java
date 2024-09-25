package edu.illinois.library.cantaloupe.resource.iiif.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Class whose instances are intended to be serialized as JSON for use in
 * information responses.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">IIIF Image
 * API 1.1</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "@context", "@id", "width", "height", "scale_factors",
        "tile_width", "tile_height", "formats", "qualities", "profile" })
@JsonInclude(JsonInclude.Include.NON_NULL)
final class Information {

    @JsonProperty("@context")
    public final String context = "http://library.stanford.edu/iiif/image-api/1.1/context.json";

    public final List<String> formats = new ArrayList<>();

    public Integer height;

    @JsonProperty("@id")
    public String id;

    public String profile;

    public final List<String> qualities = new ArrayList<>();

    @JsonProperty("scale_factors")
    public final List<Integer> scaleFactors = new ArrayList<>();

    @JsonProperty("tile_height")
    public Integer tileHeight;

    @JsonProperty("tile_width")
    public Integer tileWidth;

    public Integer width;

}
