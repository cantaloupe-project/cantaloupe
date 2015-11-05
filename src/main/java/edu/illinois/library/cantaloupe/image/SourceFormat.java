package edu.illinois.library.cantaloupe.image;

import org.restlet.data.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * Should contain constants for every source format that anyone could possibly
 * ever want to read.
 */
public enum SourceFormat {

    BMP("bmp", "BMP"),
    GIF("gif", "GIF"),
    JP2("jp2", "JPEG2000"),
    JPG("jpg", "JPEG"),
    PDF("pdf", "PDF"),
    PNG("png", "PNG"),
    TIF("tif", "TIFF"),
    WEBP("webp", "WebP"),
    UNKNOWN("unknown", "Unknown");

    private String id;
    private String name;

    /**
     * @param identifier
     * @return The source format corresponding to the given identifier,
     * assuming that its value will have a recognizable filename extension. If
     * not, <code>SourceFormat.UNKNOWN</code> will be returned.
     */
    public static SourceFormat getSourceFormat(Identifier identifier) {
        String extension = null;
        int i = identifier.getValue().lastIndexOf('.');
        if (i > 0) {
            extension = identifier.getValue().substring(i + 1);
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

    SourceFormat(String internalId, String name) {
        this.id = internalId;
        this.name = name;
    }

    public List<String> getExtensions() {
        List<String> extensions = new ArrayList<>();
        // when there are multiple extensions for a given format, the first one
        // will be the preferred extension
        switch (this.id) {
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
            case "pdf":
                types.add(new MediaType("application/pdf"));
                break;
            case "png":
                types.add(new MediaType("image/png"));
                break;
            case "tif":
                types.add(new MediaType("image/tiff"));
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

    public String getPreferredExtension() {
        return this.getExtensions().get(0);
    }

    public MediaType getPreferredMediaType() {
        return this.getMediaTypes().get(0);
    }

    /**
     * @return Preferred extension.
     */
    public String toString() {
        return this.getPreferredExtension();
    }

}
