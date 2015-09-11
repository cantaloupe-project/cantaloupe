package edu.illinois.library.cantaloupe.image;

import org.restlet.data.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * Should contain constants for every source format that anyone could possibly
 * ever want to read.
 */
public enum SourceFormat {

    BMP(new MediaType("image/bmp")),
    GIF(new MediaType("image/gif")),
    JP2(new MediaType("image/jp2")),
    JPG(new MediaType("image/jpeg")),
    PDF(new MediaType("application/pdf")),
    PNG(new MediaType("image/png")),
    TIF(new MediaType("image/tiff")),
    WEBP(new MediaType("image/webp")),
    UNKNOWN(new MediaType("unknown/unknown"));

    private MediaType mediaType;

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

    /**
     * @param mediaType Media (MIME) type.
     * @return The source format corresponding to the given media type.
     */
    public static SourceFormat getSourceFormat(MediaType mediaType) {
        for (SourceFormat enumValue : SourceFormat.values()) {
            if (enumValue.getMediaType().equals(mediaType)) {
                return enumValue;
            }
        }
        return SourceFormat.UNKNOWN;
    }

    SourceFormat(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public List<String> getExtensions() {
        List<String> extensions = new ArrayList<String>();
        String mediaType = this.getMediaType().toString();
        // the first extension will be the preferred extension
        if (mediaType.equals("application/pdf")) {
            extensions.add("pdf");
        } else if (mediaType.equals("image/bmp")) {
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
            extensions.add("ptif");
            extensions.add("tiff");
        } else if (mediaType.equals("image/webp")) {
            extensions.add("webp");
        } else if (mediaType.equals("unknown/unknown")) {
            extensions.add("unknown");
        }
        return extensions;
    }

    public MediaType getMediaType() {
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
