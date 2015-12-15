package edu.illinois.library.cantaloupe.image;

import org.restlet.data.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains constants for a wide variety of source formats, including all that
 * any processor supports.
 */
public enum SourceFormat {

    AVI("avi", "AVI", Type.VIDEO),
    BMP("bmp", "BMP", Type.IMAGE),
    GIF("gif", "GIF", Type.IMAGE),
    JP2("jp2", "JPEG2000", Type.IMAGE),
    JPG("jpg", "JPEG", Type.IMAGE),
    MOV("mov", "QuickTime", Type.VIDEO),
    MP4("mp4", "MPEG-4", Type.VIDEO),
    MPG("mpg", "MPEG", Type.VIDEO),
    PDF("pdf", "PDF", Type.IMAGE),
    PNG("png", "PNG", Type.IMAGE),
    TIF("tif", "TIFF", Type.IMAGE),
    WEBM("webm", "WebM", Type.VIDEO),
    WEBP("webp", "WebP", Type.IMAGE),
    UNKNOWN("unknown", "Unknown", null);

    public enum Type {
        IMAGE, VIDEO
    }

    private String id;
    private String name;
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
     * not, <code>SourceFormat.UNKNOWN</code> will be returned.
     */
    public static SourceFormat getSourceFormat(Identifier identifier) {
        String extension = null;
        int i = identifier.toString().lastIndexOf('.');
        if (i > 0) {
            extension = identifier.toString().substring(i + 1);
        }
        if (extension != null) {
            for (SourceFormat enumValue : SourceFormat.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    return enumValue;
                }
            }
        }
        return SourceFormat.UNKNOWN;
    }

    /**
     * @param mediaType Media (MIME) type.
     * @return The source format corresponding to the given media type, or
     * <code>SourceFormat.UNKNOWN</code> if unknown.
     */
    public static SourceFormat getSourceFormat(MediaType mediaType) {
        for (SourceFormat enumValue : SourceFormat.values()) {
            if (enumValue.getMediaTypes().contains(mediaType)) {
                return enumValue;
            }
        }
        return SourceFormat.UNKNOWN;
    }

    SourceFormat(String internalId, String name, Type type) {
        this.id = internalId;
        this.name = name;
        this.type = type;
    }

    public List<String> getExtensions() {
        List<String> extensions = new ArrayList<>();
        // when there are multiple extensions for a given format, the first one
        // will be the preferred extension
        switch (this.id) {
            case "avi":
                extensions.add("avi");
                break;
            case "bmp":
                extensions.add("bmp");
                break;
            case "gif":
                extensions.add("gif");
                break;
            case "jp2":
                extensions.add("jp2");
                break;
            case "jpg":
                extensions.add("jpg");
                extensions.add("jpeg");
                break;
            case "mov":
                extensions.add("mov");
                break;
            case "mp4":
                extensions.add("mp4");
                break;
            case "mpg":
                extensions.add("mpg");
                break;
            case "pdf":
                extensions.add("pdf");
                break;
            case "png":
                extensions.add("png");
                break;
            case "tif":
                extensions.add("tif");
                extensions.add("ptif");
                extensions.add("tiff");
                break;
            case "webm":
                extensions.add("webm");
                break;
            case "webp":
                extensions.add("webp");
                break;
            case "unknown":
                extensions.add("unknown");
                break;
        }
        return extensions;
    }

    public List<MediaType> getMediaTypes() {
        List<MediaType> types = new ArrayList<>();
        // when there are multiple types for a given format, the first one will
        // be the preferred type
        switch (this.id) {
            case "avi":
                types.add(new MediaType("video/avi"));
                types.add(new MediaType("video/msvideo"));
                types.add(new MediaType("video/x-msvideo"));
                break;
            case "bmp":
                types.add(new MediaType("image/bmp"));
                types.add(new MediaType("image/x-ms-bmp"));
                break;
            case "gif":
                types.add(new MediaType("image/gif"));
                break;
            case "jp2":
                types.add(new MediaType("image/jp2"));
                break;
            case "jpg":
                types.add(new MediaType("image/jpeg"));
                break;
            case "mov":
                types.add(new MediaType("video/quicktime"));
                types.add(new MediaType("video/x-quicktime"));
                break;
            case "mp4":
                types.add(new MediaType("video/mp4"));
                break;
            case "mpg":
                types.add(new MediaType("video/mpeg"));
                break;
            case "pdf":
                types.add(new MediaType("application/pdf"));
                break;
            case "png":
                types.add(new MediaType("image/png"));
                break;
            case "tif":
                types.add(new MediaType("image/tiff"));
                break;
            case "webm":
                types.add(new MediaType("video/webm"));
                break;
            case "webp":
                types.add(new MediaType("image/webp"));
                break;
            case "unknown":
                types.add(new MediaType("unknown/unknown"));
                break;
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
     * {@link SourceFormat.Type#IMAGE}.
     */
    public boolean isImage() {
        return (this.getType() != null && this.getType().equals(Type.IMAGE));
    }

    /**
     * Convenience method.
     *
     * @return True if the type (as returned by {@link #getType()}) is
     * {@link SourceFormat.Type#VIDEO}.
     */
    public boolean isVideo() {
        return (this.getType() != null && this.getType().equals(Type.VIDEO));
    }

    /**
     * @return Preferred extension.
     */
    public String toString() {
        return this.getPreferredExtension();
    }

}
