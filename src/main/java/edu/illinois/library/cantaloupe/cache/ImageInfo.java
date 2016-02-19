package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

/**
 * Contains image information such as image dimensions and tile sizes for the
 * purpose of caching.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "width", "height" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageInfo {

    /**
     * Image height.
     */
    public int height = 0;

    /**
     * Image width.
     */
    public int width = 0;

    public static ImageInfo fromJson(File jsonFile) throws IOException {
        return new ObjectMapper().readValue(jsonFile, ImageInfo.class);
    }

    public static ImageInfo fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, ImageInfo.class);
    }

    /**
     * No-op constructor needed by Jackson.
     */
    public ImageInfo() {}

    /**
     * @param size Image size
     */
    public ImageInfo(Dimension size) {
        this.width = size.width;
        this.height = size.height;
    }

    public ImageInfo(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImageInfo) {
            ImageInfo other = (ImageInfo) obj;
            return (other.width == this.width && other.height == this.height);
        }
        return super.equals(obj);
    }

    @JsonIgnore
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /**
     * @return JSON representation of the instance.
     * @throws JsonProcessingException
     */
    @JsonIgnore
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writer().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS).
                writeValueAsString(this);
    }

}
