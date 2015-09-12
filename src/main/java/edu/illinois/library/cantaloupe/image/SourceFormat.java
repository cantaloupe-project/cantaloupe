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
        List<String> extensions = new ArrayList<String>();
        // the first extension will be the preferred extension
        if (this.id.equals("bmp")) {
            extensions.add("bmp");
        } else if (this.id.equals("gif")) {
            extensions.add("gif");
        } else if (this.id.equals("jp2")) {
            extensions.add("jp2");
        } else if (this.id.equals("jpg")) {
            extensions.add("jpg");
            extensions.add("jpeg");
        } else if (this.id.equals("pdf")) {
            extensions.add("pdf");
        } else if (this.id.equals("png")) {
            extensions.add("png");
        } else if (this.id.equals("tif")) {
            extensions.add("tif");
            extensions.add("ptif");
            extensions.add("tiff");
        } else if (this.id.equals("webp")) {
            extensions.add("webp");
        } else if (this.id.equals("unknown")) {
            extensions.add("unknown");
        }
        return extensions;
    }

    public List<MediaType> getMediaTypes() {
        List<MediaType> types = new ArrayList<MediaType>();
        // the first type will be the preferred extension
        if (this.id.equals("bmp")) {
            types.add(new MediaType("image/bmp"));
            types.add(new MediaType("image/x-ms-bmp"));
        } else if (this.id.equals("gif")) {
            types.add(new MediaType("image/gif"));
        } else if (this.id.equals("jp2")) {
            types.add(new MediaType("image/jp2"));
        } else if (this.id.equals("jpg")) {
            types.add(new MediaType("image/jpeg"));
        } else if (this.id.equals("pdf")) {
            types.add(new MediaType("application/pdf"));
        } else if (this.id.equals("png")) {
            types.add(new MediaType("image/png"));
        } else if (this.id.equals("tif")) {
            types.add(new MediaType("image/tiff"));
        } else if (this.id.equals("webp")) {
            types.add(new MediaType("image/webp"));
        } else if (this.id.equals("unknown")) {
            types.add(new MediaType("unknown/unknown"));
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
     * @return Extension.
     */
    public String toString() {
        return this.getPreferredExtension();
    }

}
