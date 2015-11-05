package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Class whose instances are intended to be serialized to JSON for use in IIIF
 * Image Information responses.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">IIIF Image
 * API 1.1</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "@context", "@id", "width", "height", "scale_factors",
        "tile_width", "tile_height", "formats", "qualities", "profile" })
@JsonInclude(JsonInclude.Include.NON_NULL)
class ImageInfo {

    private final String context = "http://library.stanford.edu/iiif/image-api/1.1/context.json";
    private final List<String> formats = new ArrayList<>();
    private Integer height;
    private String id;
    private String profile;
    private final List<String> qualities = new ArrayList<>();
    private final List<Integer> scaleFactors = new ArrayList<>();
    private Integer tileHeight;
    private Integer tileWidth;
    private Integer width;

    @JsonProperty("@context")
    public String getContext() {
        return context;
    }

    public List<String> getFormats() {
        return formats;
    }

    public Integer getHeight() {
        return height;
    }

    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProfile() {
        return profile;
    }

    public List<String> getQualities() {
        return qualities;
    }

    @JsonProperty("scale_factors")
    public List<Integer> getScaleFactors() {
        return scaleFactors;
    }

    @JsonProperty("tile_height")
    public Integer getTileHeight() {
        return tileHeight;
    }

    @JsonProperty("tile_width")
    public Integer getTileWidth() {
        return tileWidth;
    }

    public Integer getWidth() {
        return width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setTileHeight(int height) {
        this.tileHeight = height;
    }

    public void setTileWidth(int width) {
        this.tileWidth = width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

}
