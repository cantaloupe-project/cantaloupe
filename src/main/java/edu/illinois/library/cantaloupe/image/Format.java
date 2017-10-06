package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.resolver.Resolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enum representing all source and derivative formats recognized by the
 * application, including all that any processor supports.
 */
public enum Format {

    // N.B.: For each one of these, there should be a corresponding file with
    // a name of the lowercased enum value present in the test resources
    // directory.

    /**
     * AVI video format.
     */
    AVI("AVI",
            Arrays.asList("video/avi", "video/msvideo", "video/x-msvideo"),
            Arrays.asList("avi"),
            Type.VIDEO,
            false),

    /**
     * Windows Bitmap image format.
     */
    BMP("BMP",
            Arrays.asList("image/bmp", "image/x-bmp", "image/x-ms-bmp"),
            Arrays.asList("bmp", "dib"),
            Type.IMAGE,
            true),

    /**
     * DICOM image format.
     */
    DCM("DICOM",
            Arrays.asList("application/dicom"),
            Arrays.asList("dcm", "dic"),
            Type.IMAGE,
            false),

    /**
     * CompuServe GIF image format.
     */
    GIF("GIF",
            Arrays.asList("image/gif"),
            Arrays.asList("gif"),
            Type.IMAGE,
            true),

    /**
     * JPEG2000 image format.
     */
    JP2("JPEG2000",
            Arrays.asList("image/jp2"),
            Arrays.asList("jp2", "j2k"),
            Type.IMAGE,
            true),

    /**
     * JPEG JFIF image format.
     */
    JPG("JPEG",
            Arrays.asList("image/jpeg"),
            Arrays.asList("jpg", "jpeg"),
            Type.IMAGE,
            false),

    /**
     * Apple QuickTime video format.
     */
    MOV("QuickTime",
            Arrays.asList("video/quicktime", "video/x-quicktime"),
            Arrays.asList("mov", "qt"),
            Type.VIDEO,
            false),

    /**
     * MPEG-4 video format.
     */
    MP4("MPEG-4",
            Arrays.asList("video/mp4"),
            Arrays.asList("mp4", "m4v"),
            Type.VIDEO,
            false),

    /**
     * MPEG-1 video format.
     */
    MPG("MPEG",
            Arrays.asList("video/mpeg"),
            Arrays.asList("mpg"),
            Type.VIDEO,
            false),

    /**
     * Portable Document Format.
     */
    PDF("PDF",
            Arrays.asList("application/pdf"),
            Arrays.asList("pdf"),
            Type.IMAGE,
            false),

    /**
     * Portable Network Graphics image format.
     */
    PNG("PNG",
            Arrays.asList("image/png"),
            Arrays.asList("png"),
            Type.IMAGE,
            true),

    /**
     * LizardTech MrSID image format.
     */
    SID("MrSID",
            Arrays.asList("image/x-mrsid", "image/x.mrsid",
                    "image/x-mrsid-image"),
            Arrays.asList("sid"),
            Type.IMAGE,
            true),

    /**
     * Tagged Image File Format.
     */
    TIF("TIFF",
            Arrays.asList("image/tiff"),
            Arrays.asList("tif", "ptif", "tiff"),
            Type.IMAGE,
            true),

    /**
     * WebM video format.
     */
    WEBM("WebM",
            Arrays.asList("video/webm"),
            Arrays.asList("webm"),
            Type.VIDEO,
            false),

    /**
     * WebP image format.
     */
    WEBP("WebP",
            Arrays.asList("image/webp"),
            Arrays.asList("webp"),
            Type.IMAGE,
            true),

    /**
     * Unknown format.
     */
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
     * <p>Attempts to infer a format from the given identifier.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to get
     * this information from {@link Resolver#getSourceFormat()}.</p>
     *
     * @param identifier
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} will be returned.
     */
    public static Format inferFormat(Identifier identifier) {
        String extension = null;
        int i = identifier.toString().lastIndexOf('.');
        if (i > 0) {
            extension = identifier.toString().substring(i + 1);
        }
        if (extension != null) {
            extension = extension.toLowerCase();
            for (Format enumValue : Format.values()) {
                if (enumValue.getExtensions().contains(extension)) {
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
        return mediaTypes.stream().map(MediaType::new).
                collect(Collectors.toList());
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
