package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.processor.Processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Contains JSON-serializable information about an image, including its
 * format, dimensions, embedded metadata, subimages, and tile sizes&mdash;
 * essentially a superset of characteristics of all {@link Format formats}
 * supported by the application.</p>
 *
 * <p>Instances are format-, {@link Processor}-, and endpoint-agnostic. An
 * instance describing a particular image {@link Processor#readInfo() returned
 * from one processor} should be {@link #equals equal} to an instance
 * describing the same image returned from a different processor. This
 * preserves the freedom to change processor assignments without invalidating
 * any {@link DerivativeCache#getInfo(Identifier) cached instances}.</p>
 *
 * <p>All sizes are raw pixel data sizes, disregarding orientation.</p>
 *
 * <p>Instances ultimately originate from {@link Processor#readInfo()}, but
 * subsequently they can be {@link DerivativeCache#put(Identifier, Info)
 * cached}, perhaps for a very long time. When an instance is needed, it may be
 * preferentially acquired from a cache, with a processor being consulted only
 * as a last resort. As a result, changes to the class definition need to be
 * implemented carefully so that {@link InfoDeserializer older serializations
 * remain readable}. (Otherwise, users might have to purge their cache whenever
 * the class design changes.)</p>
 *
 * <h1>History</h1>
 *
 * <p>See {@link Serialization}.</p>
 */
@JsonSerialize(using = InfoSerializer.class)
@JsonDeserialize(using = InfoDeserializer.class)
public final class Info {

    public static final class Builder {

        private final Info info;

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

        public Builder withIdentifier(Identifier identifier) {
            info.setIdentifier(identifier);
            return this;
        }

        public Builder withMetadata(Metadata metadata) {
            info.setMetadata(metadata);
            return this;
        }

        public Builder withNumResolutions(int numResolutions) {
            info.setNumResolutions(numResolutions);
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
    @JsonPropertyOrder({ "width", "height", "tileWidth", "tileHeight" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Image {

        public int width, height;
        public Integer tileWidth, tileHeight;

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Image) {
                final Image other = (Image) obj;
                return (width == other.width &&
                        height == other.height &&
                        Objects.equals(tileWidth, other.tileWidth) &&
                        Objects.equals(tileHeight, other.tileHeight));
            }
            return super.equals(obj);
        }

        /**
         * @return Physical image size.
         */
        @JsonIgnore
        public Dimension getSize() {
            return new Dimension(width, height);
        }

        /**
         * @return Physical tile size.
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
            int[] codes = new int[] { width, height, tileWidth, tileHeight };
            return Arrays.hashCode(codes);
        }

        /**
         * @param size Physical image size.
         */
        public void setSize(Dimension size) {
            width = size.intWidth();
            height = size.intHeight();
        }

        /**
         * @param tileSize Physical image tile size.
         */
        public void setTileSize(Dimension tileSize) {
            tileWidth = tileSize.intWidth();
            tileHeight = tileSize.intHeight();
        }

    }

    public enum Serialization {

        /**
         * Represents a number of older serializations from application
         * versions prior to 3.4 that are no longer supported.
         */
        VERSION_1(1),

        /**
         * <p>Added the {@code mediaType} key.</p>
         *
         * <p>Introduced in application version 3.4.</p>
         */
        VERSION_2(2),

        /**
         * <p>Added {@code numResolutions} and {@code identifier} keys.</p>
         *
         * <p>Introduced in application version 4.0.</p>
         */
        VERSION_3(3),

        /**
         * <p>Replaced the {@code orientation} key with the {@code metadata}
         * key, and added {@code applicationVersion} and {@code
         * serializationVersion} keys.</p>
         *
         * <p>Introduced in application version 5.0.</p>
         */
        VERSION_4(4);

        private final int version;

        static final Serialization CURRENT = VERSION_4;

        Serialization(int version) {
            this.version = version;
        }

        int getVersion() {
            return version;
        }
    }

    private String appVersion           = Application.getVersion();
    private Identifier identifier;
    private MediaType mediaType;
    private Metadata metadata           = new Metadata();
    private Serialization serialization = Serialization.CURRENT;

    /**
     * Ordered list of subimages. The main image is at index {@code 0}.
     */
    private final List<Image> images = new ArrayList<>(8);

    /**
     * Number of resolutions available in the image. This applies to images
     * that may not have {@link #images literal embedded subimages}, but can
     * still be decoded at reduced scale factors.
     */
    private int numResolutions = -1;

    private transient boolean isPersistable = true;

    public static Builder builder() {
        return new Builder(new Info());
    }

    public static Info fromJSON(Path jsonFile) throws IOException {
        return newMapper().readValue(jsonFile.toFile(), Info.class);
    }

    public static Info fromJSON(InputStream jsonStream) throws IOException {
        return newMapper().readValue(jsonStream, Info.class);
    }

    public static Info fromJSON(String json) throws IOException {
        return newMapper().readValue(json, Info.class);
    }

    private static ObjectMapper newMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // This module obscures Optionals from the serialization (e.g.
        // Optional.empty() maps to null rather than { isPresent: false })
        mapper.registerModule(new Jdk8Module());
        return mapper;
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
            return Objects.equals(other.getApplicationVersion(), getApplicationVersion()) &&
                    Objects.equals(other.getSerialization(), getSerialization()) &&
                    Objects.equals(other.getIdentifier(), getIdentifier()) &&
                    Objects.equals(other.getMetadata(), getMetadata()) &&
                    Objects.equals(other.getSourceFormat(), getSourceFormat()) &&
                    other.getNumResolutions() == getNumResolutions() &&
                    other.getImages().equals(getImages());
        }
        return super.equals(obj);
    }

    /**
     * @return Version of the application with which the instance was or will
     *         be serialized.
     * @since 5.0
     * @see #getSerialization
     */
    public String getApplicationVersion() {
        return appVersion;
    }

    /**
     * @return Identifier. Will be {@code null} if the instance was serialized
     *         in an application version prior to 4.0.
     * @since 4.0
     */
    public Identifier getIdentifier() {
        return identifier;
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
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * @since 5.0
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * <p>Returns the number of resolutions contained in the image.</p>
     *
     * <ul>
     *     <li>For formats like multi-resolution TIFF, this will match the size
     *     of {@link #getImages()}.</li>
     *     <li>For formats like JPEG2000, it will be {@code (number of
     *     decomposition levels) + 1}.</li>
     *     <li>For more conventional formats like JPEG, PNG, BMP, etc., it will
     *     be {@code 1}.</li>
     *     <li>For instances deserialized from a version older than 4.0, it
     *     will be {@code -1}.</li>
     * </ul>
     *
     * @return Number of resolutions contained in the image, or {@code -1} if
     *         the instance was serialized in an older application version.
     * @since 4.0
     */
    public int getNumResolutions() {
        return numResolutions;
    }

    /**
     * @since 5.0
     * @see #getApplicationVersion
     */
    public Serialization getSerialization() {
        return serialization;
    }

    /**
     * @return Size of the main image.
     */
    public Dimension getSize() {
        return getSize(0);
    }

    /**
     * @return Size of the image at the given index.
     */
    public Dimension getSize(int imageIndex) {
        return images.get(imageIndex).getSize();
    }

    /**
     * @return Source format of the image, or {@link Format#UNKNOWN} if
     *         unknown.
     */
    public Format getSourceFormat() {
        if (mediaType != null) {
            return mediaType.toFormat();
        }
        return Format.UNKNOWN;
    }

    @Override
    public int hashCode() {
        int[] codes = new int[7];
        codes[0] = getApplicationVersion().hashCode();
        codes[1] = getSerialization().hashCode();
        codes[2] = getIdentifier().hashCode();
        codes[3] = getImages().hashCode();
        codes[4] = getSourceFormat().hashCode();
        codes[5] = getNumResolutions();
        if (getMetadata() != null) {
            codes[6] = getMetadata().hashCode();
        }
        return Arrays.hashCode(codes);
    }

    /**
     * @return Whether the instance contains complete and full information
     *         about the source image.
     * @since 5.0
     */
    public boolean isPersistable() {
        return isPersistable;
    }

    /**
     * @param version Application version string. This value is not serialized
     *                (the current application version is instead).
     *
     * @since 5.0
     */
    void setApplicationVersion(String version) {
        this.appVersion = version;
    }

    /**
     * @param identifier Identifier of the image described by the instance.
     * @since 4.0
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    /**
     * For convenient deserialization.
     *
     * @see #setSourceFormat(Format)
     * @since 3.4
     */
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * @since 5.0
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @param numResolutions Number of resolutions contained in the image.
     * @since 4.0
     */
    public void setNumResolutions(int numResolutions) {
        this.numResolutions = numResolutions;
    }

    /**
     * If a {@link Processor} cannot fully {@link Processor#readInfo()
     * populate} an instance&mdash;for example, if it can't read XMP metadata
     * in order to set a complete {@link #metadata}&mdash;then it should invoke
     * this method with a {@code false} argument to make that clear.
     *
     * @since 5.0
     */
    public void setPersistable(boolean isComplete) {
        this.isPersistable = isComplete;
    }

    /**
     * @param version One of the {@link Serialization} versions. This value is
     *                not serialized (the current serialization version is
     *                instead).
     * @since 5.0
     */
    void setSerializationVersion(int version) {
        this.serialization = Arrays
                .stream(Serialization.values())
                .filter(sv -> sv.getVersion() == version)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

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
    public String toJSON() throws JsonProcessingException {
        return newMapper().writer().writeValueAsString(this);
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
    public void writeAsJSON(OutputStream os) throws IOException {
        newMapper().writer().writeValue(os, this);
    }

}
