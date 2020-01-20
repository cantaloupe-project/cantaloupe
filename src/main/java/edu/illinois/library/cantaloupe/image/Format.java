package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.source.Source;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p>File format.</p>
 *
 * <p>Common formats recognized by the application are accessible via {@code
 * public static final} variables and automatically added to the {@link
 * #knownFormats() registry of known formats}.</p>
 *
 * <p>Instances are immutable.</p>
 */
public final class Format implements Comparable<Format> {

    public enum ImageType {
        RASTER, UNKNOWN, VECTOR
    }

    public enum Type {
        IMAGE, UNKNOWN, VIDEO
    }

    // N.B.: For each one of these constants, there should be a corresponding
    // file with a name of the lowercased constant value present in the test
    // resources directory.

    /**
     * AVI video format.
     */
    public static final Format AVI = new Format(
            "avi",
            "AVI",
            ImageType.RASTER,
            List.of("video/avi", "video/msvideo", "video/x-msvideo"),
            List.of("avi"),
            Type.VIDEO,
            false);

    /**
     * Windows Bitmap image format.
     */
    public static final Format BMP = new Format(
            "bmp",
            "BMP",
            ImageType.RASTER,
            List.of("image/bmp", "image/x-bmp", "image/x-ms-bmp"),
            List.of("bmp", "dib"),
            Type.IMAGE,
            true);

    /**
     * DICOM image format.
     */
    public static final Format DCM = new Format(
            "dcm",
            "DICOM",
            ImageType.RASTER,
            List.of("application/dicom"),
            List.of("dcm", "dic"),
            Type.IMAGE,
            false);

    /**
     * Flash Video format.
     */
    public static final Format FLV = new Format(
            "flv",
            "FLV",
            ImageType.RASTER,
            List.of("video/x-flv"),
            List.of("flv", "f4v"),
            Type.VIDEO,
            false);

    /**
     * CompuServe GIF image format.
     */
    public static final Format GIF = new Format(
            "gif",
            "GIF",
            ImageType.RASTER,
            List.of("image/gif"),
            List.of("gif"),
            Type.IMAGE,
            true);

    /**
     * JPEG2000 image format.
     */
    public static final Format JP2 = new Format(
            "jp2",
            "JPEG2000",
            ImageType.RASTER,
            List.of("image/jp2"),
            List.of("jp2", "j2k", "jpx", "jpf"),
            Type.IMAGE,
            true);

    /**
     * JPEG JFIF image format.
     */
    public static final Format JPG = new Format(
            "jpg",
            "JPEG",
            ImageType.RASTER,
            List.of("image/jpeg"),
            List.of("jpg", "jpeg"),
            Type.IMAGE,
            false);

    /**
     * Apple QuickTime video format.
     */
    public static final Format MOV = new Format(
            "mov",
            "QuickTime",
            ImageType.RASTER,
            List.of("video/quicktime", "video/x-quicktime"),
            List.of("mov", "qt"),
            Type.VIDEO,
            false);

    /**
     * MPEG-4 video format.
     */
    public static final Format MP4 = new Format(
            "mp4",
            "MPEG-4",
            ImageType.RASTER,
            List.of("video/mp4"),
            List.of("mp4", "m4v"),
            Type.VIDEO,
            false);

    /**
     * MPEG-1 video format.
     */
    public static final Format MPG = new Format(
            "mpg",
            "MPEG",
            ImageType.RASTER,
            List.of("video/mpeg"),
            List.of("mpg"),
            Type.VIDEO,
            false);

    /**
     * Portable Document Format.
     */
    public static final Format PDF = new Format(
            "pdf",
            "PDF",
            ImageType.VECTOR,
            List.of("application/pdf"),
            List.of("pdf"),
            Type.IMAGE,
            false);

    /**
     * Portable Network Graphics image format.
     */
    public static final Format PNG = new Format(
            "png",
            "PNG",
            ImageType.RASTER,
            List.of("image/png"),
            List.of("png"),
            Type.IMAGE,
            true);

    /**
     * Tagged Image File Format.
     */
    public static final Format TIF = new Format(
            "tif",
            "TIFF",
            ImageType.RASTER,
            List.of("image/tiff"),
            List.of("tif", "ptif", "tiff"),
            Type.IMAGE,
            true);

    /**
     * WebM video format.
     */
    public static final Format WEBM = new Format(
            "webm",
            "WebM",
            ImageType.RASTER,
            List.of("video/webm"),
            List.of("webm"),
            Type.VIDEO,
            false);

    /**
     * WebP image format.
     */
    public static final Format WEBP = new Format(
            "webp",
            "WebP",
            ImageType.RASTER,
            List.of("image/webp"),
            List.of("webp"),
            Type.IMAGE,
            true);

    /**
     * Unknown format.
     */
    public static final Format UNKNOWN = new Format(
            "unknown",
            "Unknown",
            ImageType.UNKNOWN,
            List.of("unknown/unknown"),
            List.of("unknown"),
            Type.UNKNOWN,
            false);

    /**
     * XPM image format.
     */
    public static final Format XPM = new Format(
            "xpm",
            "XPM",
            ImageType.RASTER,
            // TODO: Tika returns image/x-xbitmap for XPMs. I thought that was
            // the type for XBM, but since we don't support that, we can let it
            // slide for now.
            List.of("image/x-xpixmap", "image/x-xbitmap"),
            List.of("xpm"),
            Type.IMAGE,
            true);

    private static final Set<Format> KNOWN_FORMATS =
            ConcurrentHashMap.newKeySet();

    private List<String> extensions;
    private ImageType imageType;
    private String key;
    private List<String> mediaTypes;
    private String name;
    private boolean supportsTransparency;
    private Type type;

    static {
        KNOWN_FORMATS.addAll(Set.of(AVI, BMP, DCM, FLV, GIF, JP2, JPG, MOV,
                MP4, MPG, PDF, PNG, TIF, UNKNOWN, WEBM, WEBP, XPM));
    }

    /**
     * <p>Attempts to infer a format from the given identifier.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to
     * obtain this information from {@link Source#getFormatIterator()}.</p>
     *
     * @param identifier
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} is returned.
     */
    public static Format inferFormat(Identifier identifier) {
        return inferFormat(identifier.toString());
    }

    /**
     * <p>Attempts to infer a format from the given pathname.</p>
     *
     * <p>It is usually more reliable (but also maybe more expensive) to
     * obtain this information from {@link Source#getFormatIterator()}.</p>
     *
     * @param pathname
     * @return The source format corresponding to the given identifier,
     *         assuming that its value will have a recognizable filename
     *         extension. If not, {@link #UNKNOWN} is returned.
     */
    public static Format inferFormat(String pathname) {
        String extension = null;
        int i = pathname.lastIndexOf('.');
        if (i > 0) {
            extension = pathname.substring(i + 1);
        }
        if (extension != null) {
            extension = extension.toLowerCase();
            for (Format format : Format.knownFormats()) {
                if (format.getExtensions().contains(extension)) {
                    return format;
                }
            }
        }
        return Format.UNKNOWN;
    }

    /**
     * @return Thread-safe registry of all known formats.
     */
    public static Set<Format> knownFormats() {
        return KNOWN_FORMATS;
    }

    /**
     * @return Format in the {@link #knownFormats() registry} with the given
     *         extension.
     */
    public static Format withExtension(String extension) {
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        final String lcext = extension.toLowerCase();
        return knownFormats()
                .stream()
                .filter(f -> f.getExtensions().contains(lcext))
                .findAny()
                .orElse(null);
    }

    public Format(String key,
                  String name,
                  ImageType imageType,
                  List<String> mediaTypes,
                  List<String> extensions,
                  Type type,
                  boolean supportsTransparency) {
        this.key                  = key;
        this.name                 = name;
        this.imageType            = imageType;
        this.mediaTypes           = mediaTypes;
        this.extensions           = extensions;
        this.type                 = type;
        this.supportsTransparency = supportsTransparency;
    }

    @Override
    public int compareTo(Format o) {
        return getName().compareTo(o.getName());
    }

    /**
     * @param obj Object to compare.
     * @return    Whether the given object's {@link #getKey() key} is equal to
     *            that of the instance.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Format) {
            Format other = (Format) obj;
            return other.key.equals(key);
        }
        return super.equals(obj);
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
     * @return Unique format key, used internally but not relevant outside of
     *         the application nor shown to users.
     * @see #getName()
     */
    public String getKey() {
        return key;
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
     * @see #getKey()
     */
    public String getName() {
        return name;
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

    @Override
    public int hashCode() {
        return key.hashCode();
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
    @Override
    public String toString() {
        return getPreferredExtension();
    }

}
