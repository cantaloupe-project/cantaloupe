package edu.illinois.library.cantaloupe.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.image.Format;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains JSON-serializable information about an image, such as dimensions,
 * number of subimages, and tile sizes.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "media_type", "images" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ImageInfo {

    @JsonPropertyOrder({ "width", "height", "tile_width",
            "tile_height", "compression" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Image {
        public int width = 0;
        public int height = 0;

        @JsonProperty("tile_width")
        public int tileWidth = 0;

        @JsonProperty("tile_height")
        public int tileHeight = 0;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Image) {
                Image other = (Image) obj;
                return (other.width == this.width &&
                        other.height == this.height &&
                        other.tileWidth == this.tileWidth &&
                        other.tileHeight == this.tileHeight);
            }
            return super.equals(obj);
        }

        @JsonIgnore
        public Dimension getSize() {
            return new Dimension(width, height);
        }

        @JsonIgnore
        public Dimension getTileSize() {
            return new Dimension(tileWidth, tileHeight);
        }

        public void setSize(Dimension size) {
            width = size.width;
            height = size.height;
        }

        public void setTileSize(Dimension tileSize) {
            tileWidth = tileSize.width;
            tileHeight = tileSize.height;
        }

    }

    /**
     * Ordered list of subimages. The main image is at index 0.
     */
    private List<Image> images = new ArrayList<>();

    @JsonProperty("media_type")
    private String mediaType;

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
     * @param size Main image size
     */
    public ImageInfo(Dimension size) {
        Image image = new Image();
        image.width = size.width;
        image.height = size.height;
        images.add(image);
    }

    /**
     * @param size Main image size
     * @param sourceFormat
     */
    public ImageInfo(Dimension size, Format sourceFormat) {
        Image image = new Image();
        image.width = size.width;
        image.height = size.height;
        images.add(image);
        setSourceFormat(sourceFormat);
    }

    /**
     * @param width Main image width
     * @param height Main image height
     */
    public ImageInfo(int width, int height) {
        Image image = new Image();
        image.width = width;
        image.height = height;
        images.add(image);
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param sourceFormat
     */
    public ImageInfo(int width, int height, Format sourceFormat) {
        Image image = new Image();
        image.width = width;
        image.height = height;
        images.add(image);
        setSourceFormat(sourceFormat);
    }

    /**
     * @param size Main image size
     * @param tileSize Main image tile size
     */
    public ImageInfo(Dimension size, Dimension tileSize) {
        Image image = new Image();
        image.width = size.width;
        image.height = size.height;
        image.tileWidth = tileSize.width;
        image.tileHeight = tileSize.height;
        images.add(image);
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param tileWidth Main image tile width
     * @param tileHeight Main image tile height
     */
    public ImageInfo(int width, int height, int tileWidth, int tileHeight) {
        Image image = new Image();
        image.width = width;
        image.height = height;
        image.tileWidth = tileWidth;
        image.tileHeight = tileHeight;
        images.add(image);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImageInfo) {
            ImageInfo other = (ImageInfo) obj;
            return other.images.equals(this.images) &&
                    other.getSourceFormat().equals(getSourceFormat());
        }
        return super.equals(obj);
    }

    public List<Image> getImages() {
        return images;
    }

    /**
     * @return Size of the main image.
     */
    @JsonIgnore
    public Dimension getSize() {
        return getSize(0);
    }

    /**
     * @param imageIndex
     * @return Size of the image at the given index.
     */
    @JsonIgnore
    public Dimension getSize(int imageIndex) {
        return images.get(imageIndex).getSize();
    }

    /**
     * @return Source format of the image, or {@link Format#UNKNOWN} if
     *         unknown.
     */
    @JsonIgnore
    public Format getSourceFormat() {
        if (mediaType != null) {
            return Format.getFormat(mediaType);
        }
        return Format.UNKNOWN;
    }

    @JsonIgnore
    public void setSourceFormat(Format sourceFormat) {
        mediaType = sourceFormat.getPreferredMediaType().toString();
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

    @Override
    public String toString() {
        try {
            return toJson();
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

}
