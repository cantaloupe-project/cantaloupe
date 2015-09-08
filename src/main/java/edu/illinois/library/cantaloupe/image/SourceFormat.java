package edu.illinois.library.cantaloupe.image;

import java.util.ArrayList;
import java.util.List;

/**
 * Should contain constants for every source format that anyone could possibly
 * ever want to read.
 */
public enum SourceFormat {

    BMP("image/bmp"),
    GIF("image/gif"),
    JP2("image/jp2"),
    JPG("image/jpeg"),
    PNG("image/png"),
    TIF("image/tiff"),
    WEBP("image/webp"),
    UNKNOWN("unknown/unknown");

    private String mediaType;

    /**
     * @param identifier IIIF identifier.
     * @return The source format corresponding to the given identifier.
     */
    public static SourceFormat getSourceFormat(String identifier) {
        String extension = null;
        int i = identifier.lastIndexOf('.');
        if (i > 0) {
            extension = identifier.substring(i + 1);
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

    SourceFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    public List<String> getExtensions() {
        List<String> extensions = new ArrayList<String>();
        String mediaType = this.getMediaType();
        if (mediaType.equals("image/bmp")) {
            extensions.add("bmp");
        } else if (mediaType.equals("image/gif")) {
            extensions.add("gif");
        } else if (mediaType.equals("image/jp2")) {
            extensions.add("jp2");
        } else if (mediaType.equals("image/jpeg")) {
            extensions.add("jpg");
            extensions.add("jpeg");
        } else if (mediaType.equals("image/png")) {
            extensions.add("png");
        } else if (mediaType.equals("image/tiff")) {
            extensions.add("tif");
            extensions.add("tiff");
        } else if (mediaType.equals("image/webp")) {
            extensions.add("webp");
        } else if (mediaType.equals("unknown/unknown")) {
            extensions.add("unknown");
        }
        return extensions;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public String getPreferredExtension() {
        return this.getExtensions().get(0);
    }

    /**
     * @return Extension.
     */
    public String toString() {
        return (String) this.getExtensions().toArray()[0];
    }

}
