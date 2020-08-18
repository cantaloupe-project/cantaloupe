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
 * #all() registry of known formats}.</p>
 *
 * <p>Instances are immutable.</p>
 */
public final class Format implements Comparable<Format> {


    // N.B.: For each one of these constants, there should be a corresponding
    // file with a name of the lowercased constant value present in the test
    // resources directory.

    /**
     * AVI video format.
     */
    public static final Format AVI = new Format(
            "avi",
            "AVI",
            List.of("video/avi", "video/msvideo", "video/x-msvideo"),
            List.of("avi"),
            true,
            true,
            false);

    /**
     * Windows Bitmap image format.
     */
    public static final Format BMP = new Format(
            "bmp",
            "BMP",
            List.of("image/bmp", "image/x-bmp", "image/x-ms-bmp"),
            List.of("bmp", "dib"),
            true,
            false,
            true);

    /**
     * Flash Video format.
     */
    public static final Format FLV = new Format(
            "flv",
            "FLV",
            List.of("video/x-flv"),
            List.of("flv", "f4v"),
            true,
            true,
            false);

    /**
     * CompuServe GIF image format.
     */
    public static final Format GIF = new Format(
            "gif",
            "GIF",
            List.of("image/gif"),
            List.of("gif"),
            true,
            false,
            true);

    /**
     * JPEG2000 image format.
     */
    public static final Format JP2 = new Format(
            "jp2",
            "JPEG2000",
            List.of("image/jp2"),
            List.of("jp2", "j2k", "jpx", "jpf"),
            true,
            false,
            true);

    /**
     * JPEG JFIF image format.
     */
    public static final Format JPG = new Format(
            "jpg",
            "JPEG",
            List.of("image/jpeg"),
            List.of("jpg", "jpeg"),
            true,
            false,
            false);

    /**
     * Apple QuickTime video format.
     */
    public static final Format MOV = new Format(
            "mov",
            "QuickTime",
            List.of("video/quicktime", "video/x-quicktime"),
            List.of("mov", "qt"),
            true,
            true,
            false);

    /**
     * MPEG-4 video format.
     */
    public static final Format MP4 = new Format(
            "mp4",
            "MPEG-4",
            List.of("video/mp4"),
            List.of("mp4", "m4v"),
            true,
            true,
            false);

    /**
     * MPEG-1 video format.
     */
    public static final Format MPG = new Format(
            "mpg",
            "MPEG",
            List.of("video/mpeg"),
            List.of("mpg"),
            true,
            true,
            false);

    /**
     * Portable Document Format.
     */
    public static final Format PDF = new Format(
            "pdf",
            "PDF",
            List.of("application/pdf"),
            List.of("pdf"),
            false,
            false,
            false);

    /**
     * Portable Network Graphics image format.
     */
    public static final Format PNG = new Format(
            "png",
            "PNG",
            List.of("image/png"),
            List.of("png"),
            true,
            false,
            true);

    /**
     * Tagged Image File Format.
     */
    public static final Format TIF = new Format(
            "tif",
            "TIFF",
            List.of("image/tiff"),
            List.of("tif", "ptif", "tiff"),
            true,
            false,
            true);

    /**
     * WebM video format.
     */
    public static final Format WEBM = new Format(
            "webm",
            "WebM",
            List.of("video/webm"),
            List.of("webm"),
            true,
            true,
            false);

    /**
     * WebP image format.
     */
    public static final Format WEBP = new Format(
            "webp",
            "WebP",
            List.of("image/webp"),
            List.of("webp"),
            true,
            false,
            true);

    public static final Format XPM = new Format(
            "xpm",
            "XPM",
            // TODO: Tika returns image/x-xbitmap for XPMs. I thought that was
            // the type for XBM, but since we don't support that, we can let it
            // slide for now.
            List.of("image/x-xpixmap", "image/x-xbitmap"),
            List.of("xpm"),
            false,
            false,
            true);

    /**
     * Represents an unknown format, in lieu of using {@code null}.
     */
    public static final Format UNKNOWN = new Format(
            "unknown",
            "Unknown",
            List.of("unknown/unknown"),
            List.of("unknown"),
            true,
            false,
            false);

    private static final Set<Format> KNOWN_FORMATS =
            ConcurrentHashMap.newKeySet();

    private List<String> extensions;
    private List<String> mediaTypes;
    private String key;
    private String name;
    private boolean isRaster;
    private boolean isVideo;
    private boolean supportsTransparency;

    static {
        KNOWN_FORMATS.addAll(Set.of(AVI, BMP, FLV, GIF, JP2, JPG, MOV,
                MP4, MPG, PDF, PNG, TIF, UNKNOWN, WEBM, WEBP, XPM));
    }

    /**
     * @return Thread-safe registry of all known formats.
     */
    public static Set<Format> all() {
        return KNOWN_FORMATS;
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
            for (Format format : Format.all()) {
                if (format.getExtensions().contains(extension)) {
                    return format;
                }
            }
        }
        return Format.UNKNOWN;
    }

    /**
     * @return Format in the {@link #all() registry} with the given
     *         extension.
     */
    public static Format withExtension(String extension) {
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        final String lcext = extension.toLowerCase();
        return all()
                .stream()
                .filter(f -> f.getExtensions().contains(lcext))
                .findAny()
                .orElse(null);
    }

    public Format(String key,
                  String name,
                  List<String> mediaTypes,
                  List<String> extensions,
                  boolean isRaster,
                  boolean isVideo,
                  boolean supportsTransparency) {
        this.key                  = key;
        this.name                 = name;
        this.mediaTypes           = mediaTypes;
        this.extensions           = extensions;
        this.isRaster             = isRaster;
        this.isVideo              = isVideo;
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

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public boolean isRaster() {
        return isRaster;
    }

    public boolean isVideo() {
        return isVideo;
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
