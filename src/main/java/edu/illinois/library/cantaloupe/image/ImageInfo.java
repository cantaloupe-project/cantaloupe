package edu.illinois.library.cantaloupe.image;

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
 * API 2.0</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "@context", "@id", "protocol", "width", "height", "sizes",
        "tiles", "profile", "service" })
public class ImageInfo {

    private final String context = "http://iiif.io/api/image/2/context.json";
    private Integer height;
    private String id;
    private final List<Object> profile = new ArrayList<Object>();
    private final String protocol = "http://iiif.io/api/image";
    private List<Map<String,Integer>> sizes;
    private List<Map<String,Object>> tiles;
    private Integer width;

    public ImageInfo() {
        this.getProfile().add("http://iiif.io/api/image/2/level2.json");
    }

    @JsonProperty("@context")
    public String getContext() {
        return context;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Object> getProfile() {
        return profile;
    }

    public String getProtocol() {
        return protocol;
    }

    public List<Map<String, Integer>> getSizes() {
        return sizes;
    }

    public List<Map<String, Object>> getTiles() {
        return tiles;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setSizes(List<Map<String, Integer>> sizes) {
        this.sizes = sizes;
    }

    public void setTiles(List<Map<String, Object>> tiles) {
        this.tiles = tiles;
    }

}
