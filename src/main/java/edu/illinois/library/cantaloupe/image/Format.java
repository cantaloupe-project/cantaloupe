package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.source.Source;

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
            ImageType.RASTER,
            List.of("video/avi", "video/msvideo", "video/x-msvideo"),
            List.of("avi"),
            Type.VIDEO,
            false),

    /**
     * Windows Bitmap image format.
     */
    BMP("BMP",
            ImageType.RASTER,
            List.of("image/bmp", "image/x-bmp", "image/x-ms-bmp"),
            List.of("bmp", "dib"),
            Type.IMAGE,
            true),

    /**
     * DICOM image format.
     */
    DCM("DICOM",
            ImageType.RASTER,
            List.of("application/dicom"),
            List.of("dcm", "dic"),
            Type.IMAGE,
            false),

    /**
     * Flash Video format.
     */
    FLV("FLV",
            ImageType.RASTER,
            List.of("video/x-flv"),
            List.of("flv", "f4v"),
            Type.VIDEO,
            false),

    /**
     * CompuServe GIF image format.
     */
    GIF("GIF",
            ImageType.RASTER,
            List.of("image/gif"),
            List.of("gif"),
            Type.IMAGE,
            true),

    /**
     * JPEG2000 image format.
     */
    JP2("JPEG2000",
            ImageType.RASTER,
            List.of("image/jp2"),
            List.of("jp2", "j2k", "jpx", "jpf"),
            Type.IMAGE,
            true),

    /**
     * JPEG JFIF image format.
     */
    JPG("JPEG",
            ImageType.RASTER,
            List.of("image/jpeg"),
            List.of("jpg", "jpeg"),
            Type.IMAGE,
            false),

    /**
     * Apple QuickTime video format.
     */
    MOV("QuickTime",
            ImageType.RASTER,
            List.of("video/quicktime", "video/x-quicktime"),
            List.of("mov", "qt"),
            Type.VIDEO,
            false),

    /**
     * MPEG-4 video format.
     */
    MP4("MPEG-4",
            ImageType.RASTER,
            List.of("video/mp4"),
            List.of("mp4", "m4v"),
            Type.VIDEO,
            false),

    /**
     * MPEG-1 video format.
     */
    MPG("MPEG",
            ImageType.RASTER,
            List.of("video/mpeg"),
            List.of("mpg"),
            Type.VIDEO,
            false),

    /**
     * Portable Document Format.
     */
    PDF("PDF",
            ImageType.VECTOR,
            List.of("application/pdf"),
            List.of("pdf"),
            Type.IMAGE,
            false),

    /**
     * Portable Network Graphics image format.
     */
    PNG("PNG",
            ImageType.RASTER,
            List.of("image/png"),
            List.of("png"),
            Type.IMAGE,
            true),

    /**
     * Tagged Image File Format.
     */
    TIF("TIFF",
            ImageType.RASTER,
            List.of("image/tiff"),
            List.of("tif", "ptif", "tiff"),
            Type.IMAGE,
            true),

    /**
     * WebM video format.
     */
    WEBM("WebM",
            ImageType.RASTER,
            List.of("video/webm"),
            List.of("webm"),
            Type.VIDEO,
            false),

    /**
     * WebP image format.
     */
    WEBP("WebP",
            ImageType.RASTER,
            List.of("image/webp"),
            List.of("webp"),
            Type.IMAGE,
            true),

    /**
     * Unknown format.
     */
    UNKNOWN("Unknown",
            ImageType.UNKNOWN,
            List.of("unknown/unknown"),
            List.of("unknown"),
            Type.UNKNOWN,
            false);

    public enum ImageType {
        RASTER, UNKNOWN, VECTOR
    }

    public enum Type {
        IMAGE, UNKNOWN, VIDEO
    }

    private List<String> extensions;
    private ImageType imageType;
    private List<String> mediaTypes;
    private String name;
    private boolean supportsTransparency;
    private Type type;

    /**
     * <p>Attempts to infer a format from the given identifier.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to
     * obtain this information from {@link Source#getFormat()}.</p>
     *
     * @param identifier
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} will be returned.
     */
    public static Format inferFormat(Identifier identifier) {
        return inferFormat(identifier.toString());
    }

    /**
     * <p>Attempts to infer a format from the given pathname.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to
     * obtain this information from {@link Source#getFormat()}.</p>
     *
     * @param pathname
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} will be returned.
     */
    public static Format inferFormat(String pathname) {
        String extension = null;
        int i = pathname.lastIndexOf('.');
        if (i > 0) {
            extension = pathname.substring(i + 1);
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
           final ImageType imageType,
           final List<String> mediaTypes,
           final List<String> extensions,
           final Type type,
           final boolean supportsTransparency) {
        this.imageType = imageType;
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

    public ImageType getImageType() {
        return imageType;
    }

    /**
     * @return All media types associated with this format.
     * @see #getPreferredMediaType()
     */
    public List<MediaType> getMediaTypes() {
        return mediaTypes.stream().map(MediaType::new).
                collect(Collectors.toUnmodifiableList());
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
        return getExtensions().get(0);
    }

    /**
     * @return The most appropriate media type for the format.
     */
    public MediaType getPreferredMediaType() {
        return getMediaTypes().get(0);
    }

    public Type getType() {
        return type;
    }

    public boolean supportsTransparency() {
        return supportsTransparency;
    }

    /**
     * @return Unmodifiable map with {@code extension} and {@code media_type}
     *         keys corresponding to string values.
     */
    public Map<String,Object> toMap() {
        return Map.of(
                "extension", getPreferredExtension(),
                "media_type", getPreferredMediaType().toString());
    }

    /**
     * @return Preferred extension.
     */
    public String toString() {
        return this.getPreferredExtension();
    }

}
