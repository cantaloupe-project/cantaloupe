package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.request.Identifier;
import org.restlet.data.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * Should contain constants for every source format that any processor could
 * possibly consider supporting.
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
     * @param identifier IIIF identifier.
     * @return The source format corresponding to the given identifier.
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

    SourceFormat(String internalId, String name, Type type) {
        this.id = internalId;
        this.name = name;
        this.type = type;
    }

    public List<String> getExtensions() {
        List<String> extensions = new ArrayList<>();
        // the first extension will be the preferred extension
        if (this.id.equals("avi")) {
            extensions.add("avi");
        } else if (this.id.equals("bmp")) {
            extensions.add("bmp");
        } else if (this.id.equals("gif")) {
            extensions.add("gif");
        } else if (this.id.equals("jp2")) {
            extensions.add("jp2");
        } else if (this.id.equals("jpg")) {
            extensions.add("jpg");
            extensions.add("jpeg");
        } else if (this.id.equals("mov")) {
            extensions.add("mov");
        } else if (this.id.equals("mp4")) {
            extensions.add("mp4");
        } else if (this.id.equals("mpg")) {
            extensions.add("mpg");
        } else if (this.id.equals("pdf")) {
            extensions.add("pdf");
        } else if (this.id.equals("png")) {
            extensions.add("png");
        } else if (this.id.equals("tif")) {
            extensions.add("tif");
            extensions.add("ptif");
            extensions.add("tiff");
        } else if (this.id.equals("webm")) {
            extensions.add("webm");
        } else if (this.id.equals("webp")) {
            extensions.add("webp");
        } else if (this.id.equals("unknown")) {
            extensions.add("unknown");
        }
        return extensions;
    }

    public List<MediaType> getMediaTypes() {
        List<MediaType> types = new ArrayList<>();
        // the first type will be the preferred extension
        if (this.id.equals("avi")) {
            types.add(new MediaType("video/avi"));
            types.add(new MediaType("video/msvideo"));
            types.add(new MediaType("video/x-msvideo"));
        } if (this.id.equals("bmp")) {
            types.add(new MediaType("image/bmp"));
            types.add(new MediaType("image/x-ms-bmp"));
        } else if (this.id.equals("gif")) {
            types.add(new MediaType("image/gif"));
        } else if (this.id.equals("jp2")) {
            types.add(new MediaType("image/jp2"));
        } else if (this.id.equals("jpg")) {
            types.add(new MediaType("image/jpeg"));
        } else if (this.id.equals("mov")) {
            types.add(new MediaType("video/quicktime"));
            types.add(new MediaType("video/x-quicktime"));
        } else if (this.id.equals("mp4")) {
            types.add(new MediaType("video/mp4"));
        } else if (this.id.equals("mpg")) {
            types.add(new MediaType("video/mpeg"));
        } else if (this.id.equals("pdf")) {
            types.add(new MediaType("application/pdf"));
        } else if (this.id.equals("png")) {
            types.add(new MediaType("image/png"));
        } else if (this.id.equals("tif")) {
            types.add(new MediaType("image/tiff"));
        } else if (this.id.equals("webm")) {
            types.add(new MediaType("video/webm"));
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

    public Type getType() {
        return this.type;
    }

    /**
     * @return Extension.
     */
    public String toString() {
        return this.getPreferredExtension();
    }

}
