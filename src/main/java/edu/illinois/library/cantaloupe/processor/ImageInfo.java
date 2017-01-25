package edu.illinois.library.cantaloupe.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Orientation;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Contains JSON-serializable information about an image, such as dimensions,
 * orientation, its subimages, and tile sizes.</p>
 *
 * <p>All sizes are raw pixel data sizes, disregarding orientation.</p>
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 * docs</a>
 */
@JsonPropertyOrder({ "mediaType", "images" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ImageInfo {

    @JsonPropertyOrder({ "width", "height", "tileWidth", "tileHeight",
            "orientation" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Image {
        public int width = 0;
        public int height = 0;
        public String orientation;
        public Integer tileWidth;
        public Integer tileHeight;

        /**
         * No-op constructor.
         */
        public Image() {
        }

        public Image(Dimension size) {
            this.setSize(size);
        }

        public Image(Dimension size, Orientation orientation) {
            this(size);
            setOrientation(orientation);
        }

        public Image(int width, int height) {
            this(new Dimension(width, height));
        }

        public Image(int width, int height, Orientation orientation) {
            this(new Dimension(width, height), orientation);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Image) {
                final Image other = (Image) obj;
                if (other.width == this.width && other.height == this.height) {
                    if (this.tileWidth != null && other.tileWidth != null &&
                            this.tileWidth.equals(other.tileWidth) &&
                            this.tileHeight != null && other.tileHeight != null &&
                            this.tileHeight.equals(other.tileHeight)) {
                        return true;
                    } else if (this.tileWidth == null && other.tileWidth == null &&
                            this.tileHeight == null && other.tileHeight == null) {
                        return true;
                    }
                }
            }
            return super.equals(obj);
        }

        @JsonIgnore
        public Orientation getOrientation() {
            if (orientation != null) {
                return Orientation.valueOf(orientation);
            }
            return Orientation.ROTATE_0;
        }

        /**
         * @return Image size with the orientation taken into account.
         */
        @JsonIgnore
        public Dimension getOrientationSize() {
            Dimension size = getSize();
            if (getOrientation().equals(Orientation.ROTATE_90) ||
                    getOrientation().equals(Orientation.ROTATE_270)) {
                final int tmp = size.width;
                size.width = size.height;
                size.height = tmp;
            }
            return size;
        }

        /**
         * @return Tile size with the orientation taken into account.
         */
        @JsonIgnore
        public Dimension getOrientationTileSize() {
            Dimension tileSize = getTileSize();
            if (getOrientation().equals(Orientation.ROTATE_90) ||
                    getOrientation().equals(Orientation.ROTATE_270)) {
                final int tmp = tileSize.width;
                tileSize.width = tileSize.height;
                tileSize.height = tmp;
            }
            return tileSize;
        }

        /**
         * @return Actual image size, disregarding orientation.
         */
        @JsonIgnore
        public Dimension getSize() {
            return new Dimension(width, height);
        }

        /**
         * @return Actual tile size, disregarding orientation.
         */
        @JsonIgnore
        public Dimension getTileSize() {
            if (tileWidth != null && tileHeight != null) {
                return new Dimension(tileWidth, tileHeight);
            }
            return new Dimension(width, height);
        }

        public void setOrientation(Orientation orientation) {
            this.orientation = orientation.toString();
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
    private String mediaType;

    public static ImageInfo fromJson(File jsonFile) throws IOException {
        return new ObjectMapper().readValue(jsonFile, ImageInfo.class);
    }

    public static ImageInfo fromJson(InputStream jsonStream) throws IOException {
        return new ObjectMapper().readValue(jsonStream, ImageInfo.class);
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
        images.add(new Image(size));
    }

    /**
     * @param size Main image size
     * @param sourceFormat
     */
    public ImageInfo(Dimension size, Format sourceFormat) {
        images.add(new Image(size));
        setSourceFormat(sourceFormat);
    }

    /**
     * @param width Main image width
     * @param height Main image height
     */
    public ImageInfo(int width, int height) {
        images.add(new Image(width, height));
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param sourceFormat
     */
    public ImageInfo(int width, int height, Format sourceFormat) {
        images.add(new Image(width, height));
        setSourceFormat(sourceFormat);
    }

    /**
     * @param size Main image size
     * @param tileSize Main image tile size
     */
    public ImageInfo(Dimension size, Dimension tileSize) {
        Image image = new Image(size);
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
        Image image = new Image(width, height);
        image.tileWidth = tileWidth;
        image.tileHeight = tileHeight;
        images.add(image);
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param tileWidth Main image tile width
     * @param tileHeight Main image tile height
     * @param sourceFormat
     */
    public ImageInfo(int width, int height, int tileWidth, int tileHeight,
                     Format sourceFormat) {
        Image image = new Image(width, height);
        image.tileWidth = tileWidth;
        image.tileHeight = tileHeight;
        images.add(image);
        setSourceFormat(sourceFormat);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImageInfo) {
            ImageInfo other = (ImageInfo) obj;
            return other.getImages().equals(getImages()) &&
                    other.getSourceFormat().equals(getSourceFormat());
        }
        return super.equals(obj);
    }

    public List<Image> getImages() {
        return images;
    }

    /**
     * @return Orientatino of the main image.
     */
    @JsonIgnore
    public Orientation getOrientation() {
        return images.get(0).getOrientation();
    }

    /**
     * @return Size of the main image, respecting its orientation.
     */
    @JsonIgnore
    public Dimension getOrientationSize() {
        return getOrientationSize(0);
    }

    /**
     * @param imageIndex
     * @return Size of the image at the given index, respecting its
     *         orientation.
     */
    @JsonIgnore
    public Dimension getOrientationSize(int imageIndex) {
        return images.get(imageIndex).getOrientationSize();
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
            return Format.inferFormat(mediaType);
        }
        return Format.UNKNOWN;
    }

    @JsonIgnore
    public void setSourceFormat(Format sourceFormat) {
        if (sourceFormat == null) {
            mediaType = null;
        } else {
            mediaType = sourceFormat.getPreferredMediaType().toString();
        }
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

    /**
     * @param os Output stream to write to.
     * @throws IOException
     */
    @JsonIgnore
    public void writeAsJson(OutputStream os) throws IOException {
        new ObjectMapper().writer().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS).
                writeValue(os, this);
    }

}
