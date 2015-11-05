package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

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

        private Integer height;
        private Integer width;

        public Size() {
        }

        public Size(Integer width, Integer height) {
            this.setWidth(width);
            this.setHeight(height);
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

    }

    private final String context = "http://iiif.io/api/image/2/context.json";
    private Integer height;
    private String id;
    private final List<Object> profile = new ArrayList<>();
    private final String protocol = "http://iiif.io/api/image";
    private List<Size> sizes = new ArrayList<>();
    private Integer width;

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

    public List<Size> getSizes() {
        return sizes;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

}
