package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.processor.Processor;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Contains JSON-serializable information about an image, including its
 * format, dimensions, orientation, subimages, and tile sizes&mdash;
 * essentially a superset of characteristics of all {@link Format formats}
 * supported by the application.</p>
 *
 * <p>Instances are format- and processor-agnostic. An instance describing a
 * particular image {@link Processor#readImageInfo() returned from one
 * processor} should be {@link #equals(Object) equal} to an instance describing
 * the same image returned from a different processor. This preserves the
 * freedom to change processor assignments without invalidating any
 * {@link DerivativeCache#getImageInfo(Identifier) cached instances}.</p>
 *
 * <p>All sizes are raw pixel data sizes, disregarding orientation.</p>
 *
 * <p>Instances ultimately originate from {@link Processor#readImageInfo()},
 * but subsequently they can be {@link DerivativeCache#put(Identifier, Info)
 * cached}, perhaps for a very long time. For efficiency's sake, when an
 * instance is needed, it will be preferentially acquired from a cache, and a
 * processor will be consulted only as a last resort. As a result, changes to
 * the class definition need to be implemented carefully so that older
 * serializations remain d{@link Processor#readImageInfo() readable}.
 * Otherwise, users would have to purge their cache whenever the class design
 * changes.)</p>
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
 *      docs</a>
 */
@JsonPropertyOrder({ "mediaType", "images" })
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Info {

    public static final class Builder {

        private Info info;

        Builder(Info info) {
            this.info = info;
        }

        public Info build() {
            return info;
        }

        public Builder withFormat(Format format) {
            info.setSourceFormat(format);
            return this;
        }

        public Builder withNumResolutions(int numResolutions) {
            info.setNumResolutions(numResolutions);
            return this;
        }

        public Builder withOrientation(Orientation orientation) {
            info.getImages().get(0).setOrientation(orientation);
            return this;
        }

        public Builder withSize(Dimension size) {
            info.getImages().get(0).setSize(size);
            return this;
        }

        public Builder withSize(int width, int height) {
            return withSize(new Dimension(width, height));
        }

        public Builder withTileSize(Dimension size) {
            info.getImages().get(0).setTileSize(size);
            return this;
        }

        public Builder withTileSize(int width, int height) {
            return withTileSize(new Dimension(width, height));
        }

    }

    /**
     * Represents an embedded subimage within a container stream. This is a
     * physical image such as an embedded EXIF thumbnail or embedded TIFF page.
     */
    @JsonPropertyOrder({ "width", "height", "tileWidth", "tileHeight",
            "orientation" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Image {

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

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Image) {
                final Image other = (Image) obj;
                return (width == other.width && height == other.height &&
                        Objects.equals(tileWidth, other.tileWidth) &&
                        Objects.equals(tileHeight, other.tileHeight) &&
                        Objects.equals(orientation, other.orientation));
            }
            return super.equals(obj);
        }

        /**
         * @return The instance's orientation, or {@link Orientation#ROTATE_0}
         *         if none.
         */
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
         * @return Physical image size, disregarding orientation.
         */
        @JsonIgnore
        public Dimension getSize() {
            return new Dimension(width, height);
        }

        /**
         * @return Physical tile size, disregarding orientation.
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
            return String.format("%d%d%s%d%d", width, height,
                    orientation, tileWidth, tileHeight).hashCode();
        }

        public void setOrientation(Orientation orientation) {
            this.orientation = orientation.toString();
        }

        /**
         * @param size Physical image size, disregarding orientation.
         */
        public void setSize(Dimension size) {
            width = size.width;
            height = size.height;
        }

        /**
         * @param tileSize Physical image tile size, disregarding orientation.
         */
        public void setTileSize(Dimension tileSize) {
            tileWidth = tileSize.width;
            tileHeight = tileSize.height;
        }

    }

    /**
     * Ordered list of subimages. The main image is at index {@literal 0}.
     */
    private final List<Image> images = new ArrayList<>();

    private MediaType mediaType;

    /**
     * Number of resolutions available in the image. This applies to images
     * that may not have literal embedded subimages, but can be decoded at
     * reduced scale multiples.
     */
    private int numResolutions = -1;

    public static Builder builder() {
        return new Builder(new Info());
    }

    public static Info fromJSON(Path jsonFile) throws IOException {
        return new ObjectMapper().readValue(jsonFile.toFile(), Info.class);
    }

    public static Info fromJSON(InputStream jsonStream) throws IOException {
        return new ObjectMapper().readValue(jsonStream, Info.class);
    }

    public static Info fromJSON(String json) throws IOException {
        return new ObjectMapper().readValue(json, Info.class);
    }

    public Info() {
        images.add(new Image());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Info) {
            Info other = (Info) obj;
            return other.getImages().equals(getImages()) &&
                    other.getOrientation().equals(getOrientation()) &&
                    other.getSourceFormat().equals(getSourceFormat()) &&
                    other.getNumResolutions() == getNumResolutions();
        }
        return super.equals(obj);
    }

    public List<Image> getImages() {
        return images;
    }

    /**
     * For convenient serialization.
     *
     * @see #getSourceFormat()
     * @since 3.4
     */
    @JsonGetter
    @SuppressWarnings("unused")
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * <p>Returns the number of resolutions contained in the image.</p>
     *
     * <ul>
     *     <li>For formats like multi-resolution {@link Format#TIF}, this will
     *     match the size of {@link #getImages()}.</li>
     *     <li>For formats like {@link Format#JP2}, it will be {@literal
     *     number of decomposition levels + 1}.</li>
     *     <li>For more conventional formats like {@link Format#JPG},
     *     {@link Format#PNG}, {@link Format#BMP}, etc., it will be
     *     {@literal 1}.</li>
     *     <li>For instances deserialized from a version older than 4.0, it
     *     will be {@literal -1}.</li>
     * </ul>
     *
     * @return Number of resolutions contained in the image.
     * @since 4.0
     */
    @JsonGetter
    public int getNumResolutions() {
        return numResolutions;
    }

    /**
     * @return Orientation of the main image.
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
            return mediaType.toFormat();
        }
        return Format.UNKNOWN;
    }

    @Override
    public int hashCode() {
        return String.format("%d%d",
                getImages().hashCode(),
                getSourceFormat().hashCode()).hashCode();
    }

    /**
     * For convenient deserialization.
     *
     * @see #setSourceFormat(Format)
     * @since 3.4
     */
    @JsonSetter
    @SuppressWarnings("unused")
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * @param numResolutions Number of resolutions contained in the image.
     * @since 4.0
     */
    @JsonSetter
    public void setNumResolutions(int numResolutions) {
        this.numResolutions = numResolutions;
    }

    @JsonIgnore
    public void setSourceFormat(Format sourceFormat) {
        if (sourceFormat == null) {
            mediaType = null;
        } else {
            mediaType = sourceFormat.getPreferredMediaType();
        }
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
        } catch (JsonProcessingException e) { // this should never happen
            return super.toString();
        }
    }

    /**
     * @param os Output stream to write to.
     */
    @JsonIgnore
    public void writeAsJSON(OutputStream os) throws IOException {
        new ObjectMapper().writer().writeValue(os, this);
    }

}
