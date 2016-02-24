package edu.illinois.library.cantaloupe.image;

import org.restlet.data.MediaType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains constants for a wide variety of source formats, including all that
 * any processor supports.
 */
public enum Format {

    AVI("AVI",
            Arrays.asList("video/avi", "video/msvideo", "video/x-msvideo"),
            Arrays.asList("avi"),
            Type.VIDEO,
            false),
    BMP("BMP",
            Arrays.asList("image/bmp", "image/x-bmp", "image/x-ms-bmp"),
            Arrays.asList("bmp", "dib"),
            Type.IMAGE,
            true),
    GIF("GIF",
            Arrays.asList("image/gif"),
            Arrays.asList("gif"),
            Type.IMAGE,
            true),
    JP2("JPEG2000",
            Arrays.asList("image/jp2"),
            Arrays.asList("jp2", "j2k"),
            Type.IMAGE,
            true),
    JPG("JPEG",
            Arrays.asList("image/jpeg"),
            Arrays.asList("jpg", "jpeg"),
            Type.IMAGE,
            false),
    MOV("QuickTime",
            Arrays.asList("video/quicktime", "video/x-quicktime"),
            Arrays.asList("mov"),
            Type.VIDEO,
            false),
    MP4("MPEG-4",
            Arrays.asList("video/mp4"),
            Arrays.asList("mp4", "m4v"),
            Type.VIDEO,
            false),
    MPG("MPEG",
            Arrays.asList("video/mpeg"),
            Arrays.asList("mpg"),
            Type.VIDEO,
            false),
    PDF("PDF",
            Arrays.asList("application/pdf"),
            Arrays.asList("pdf"),
            Type.IMAGE,
            false),
    PNG("PNG",
            Arrays.asList("image/png"),
            Arrays.asList("png"),
            Type.IMAGE,
            true),
    TIF("TIFF",
            Arrays.asList("image/tiff"),
            Arrays.asList("tif", "ptif", "tiff"),
            Type.IMAGE,
            true),
    WEBM("WebM",
            Arrays.asList("video/webm"),
            Arrays.asList("webm"),
            Type.VIDEO,
            false),
    WEBP("WebP",
            Arrays.asList("image/webp"),
            Arrays.asList("webp"),
            Type.IMAGE,
            true),
    UNKNOWN("Unknown",
            Arrays.asList("unknown/unknown"),
            Arrays.asList("unknown"),
            null,
            false);

    public enum Type {
        IMAGE, VIDEO
    }

    private List<String> extensions;
    private List<String> mediaTypes;
    private String name;
    private boolean supportsTransparency;
    private Type type;

    /**
     * <p>Attempts to infer a source format from the given identifier.</p>
     *
     * <p>(It is usually more reliable (but also perhaps more expensive) to get
     * this information from a
     * {@link edu.illinois.library.cantaloupe.resolver.Resolver}.)</p>
     *
     * @param identifier
     * @return The source format corresponding to the given identifier,
     * assuming that its value will have a recognizable filename extension. If
     * not, <code>Format.UNKNOWN</code> will be returned.
     */
    public static Format getFormat(Identifier identifier) {
        String extension = null;
        int i = identifier.toString().lastIndexOf('.');
        if (i > 0) {
            extension = identifier.toString().substring(i + 1);
        }
        if (extension != null) {
            for (Format enumValue : Format.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    return enumValue;
                }
            }
        }
        return Format.UNKNOWN;
    }

    /**
     * @param mediaType Media (MIME) type.
     * @return The source format corresponding to the given media type, or
     * <code>Format.UNKNOWN</code> if unknown.
     */
    public static Format getFormat(String mediaType) {
        for (Format enumValue : Format.values()) {
            for (MediaType type : enumValue.getMediaTypes()) {
                if (type.toString().equals(mediaType)) {
                    return enumValue;
                }
            }
        }
        return Format.UNKNOWN;
    }

    Format(final String name,
           final List<String> mediaTypes,
           final List<String> extensions,
           final Type type,
           final boolean supportsTransparency) {
        this.mediaTypes = mediaTypes;
        this.extensions = extensions;
        this.name = name;
        this.type = type;
        this.supportsTransparency = supportsTransparency;
    }

    /**
     * @return All extensions associated with this format.
     * @see #getPreferredExtension()
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * @return All media types associated with this format.
     * @see #getPreferredMediaType()
     */
    public List<MediaType> getMediaTypes() {
        final List<MediaType> types = new ArrayList<>();
        for (String typeStr : mediaTypes) {
            types.add(new MediaType(typeStr));
        }
        return types;
    }

    /**
     * @return Human-readable name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The most appropriate extension for the format.
     */
    public String getPreferredExtension() {
        return this.getExtensions().get(0);
    }

    /**
     * @return The most appropriate media type for the format.
     */
    public MediaType getPreferredMediaType() {
        return this.getMediaTypes().get(0);
    }

    public Type getType() {
        return this.type;
    }

    /**
     * Convenience method.
     *
     * @return True if the type (as returned by {@link #getType()}) is
     * {@link Format.Type#IMAGE}.
     */
    public boolean isImage() {
        return (this.getType() != null && this.getType().equals(Type.IMAGE));
    }

    /**
     * Convenience method.
     *
     * @return True if the type (as returned by {@link #getType()}) is
     * {@link Format.Type#VIDEO}.
     */
    public boolean isVideo() {
        return (this.getType() != null && this.getType().equals(Type.VIDEO));
    }

    public boolean supportsTransparency() {
        return this.supportsTransparency;
    }

    /**
     * @return Map serialization with <code>extension</code> and
     * <code>media_type</code> keys corresponding to string values.
     */
    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<>();
        map.put("extension", getPreferredExtension());
        map.put("media_type", getPreferredMediaType().toString());
        return map;
    }

    /**
     * @return Preferred extension.
     */
    public String toString() {
        return this.getPreferredExtension();
    }

}
