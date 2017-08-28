package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public final class Info {

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
            setOrientation(Orientation.ROTATE_0);
        }

        public Image(Dimension size) {
            this();
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

        @Override
        public int hashCode() {
            return String.format("%d_%d_%s_%d_%d", width, height,
                    orientation, tileWidth, tileHeight).hashCode();
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

    public static Info fromJSON(File jsonFile) throws IOException {
        return new ObjectMapper().readValue(jsonFile, Info.class);
    }

    public static Info fromJSON(InputStream jsonStream) throws IOException {
        return new ObjectMapper().readValue(jsonStream, Info.class);
    }

    public static Info fromJSON(String json) throws IOException {
        return new ObjectMapper().readValue(json, Info.class);
    }

    /**
     * No-op constructor needed by Jackson.
     */
    public Info() {}

    /**
     * @param size Main image size
     */
    public Info(Dimension size) {
        this();
        images.add(new Image(size));
    }

    /**
     * @param size Main image size
     * @param sourceFormat
     */
    public Info(Dimension size, Format sourceFormat) {
        this(size);
        setSourceFormat(sourceFormat);
    }

    /**
     * @param width Main image width
     * @param height Main image height
     */
    public Info(int width, int height) {
        this(new Dimension(width, height));
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param sourceFormat
     */
    public Info(int width, int height, Format sourceFormat) {
        this(width, height);
        setSourceFormat(sourceFormat);
    }

    /**
     * @param size Main image size
     * @param tileSize Main image tile size
     */
    public Info(Dimension size, Dimension tileSize) {
        this(size);
        final Image image = getImages().get(0);
        image.tileWidth = tileSize.width;
        image.tileHeight = tileSize.height;
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param tileWidth Main image tile width
     * @param tileHeight Main image tile height
     */
    public Info(int width, int height, int tileWidth, int tileHeight) {
        this(new Dimension(width, height),
                new Dimension(tileWidth, tileHeight));
    }

    /**
     * @param width Main image width
     * @param height Main image height
     * @param tileWidth Main image tile width
     * @param tileHeight Main image tile height
     * @param sourceFormat
     */
    public Info(int width, int height, int tileWidth, int tileHeight,
                Format sourceFormat) {
        this(new Dimension(width, height),
                new Dimension(tileWidth, tileHeight));
        setSourceFormat(sourceFormat);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Info) {
            Info other = (Info) obj;
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
            return new MediaType(mediaType).toFormat();
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

    @Override
    public int hashCode() {
        return new Long(getImages().hashCode() + mediaType.hashCode() +
                getSourceFormat().hashCode()).hashCode();
    }

    /**
     * @return JSON representation of the instance.
     */
    @JsonIgnore
    public String toJSON() throws JsonProcessingException {
        return new ObjectMapper().writer().writeValueAsString(this);
    }

    @Override
    public String toString() {
        try {
            return toJSON();
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

    /**
     * @param os Output stream to write to.
     */
    @JsonIgnore
    public void writeAsJson(OutputStream os) throws IOException {
        new ObjectMapper().writer().writeValue(os, this);
    }

}
