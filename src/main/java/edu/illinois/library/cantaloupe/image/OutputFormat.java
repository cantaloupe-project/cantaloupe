package edu.illinois.library.cantaloupe.image;

import java.util.HashMap;
import java.util.Map;

public enum OutputFormat {

    GIF("gif", "image/gif"),
    JP2("jp2", "image/jp2"),
    JPG("jpg", "image/jpeg"),
    PDF("pdf", "application/pdf"),
    PNG("png", "image/png"),
    TIF("tif", "image/tiff"),
    WEBP("webp", "image/webp");

    private String extension;
    private String mediaType;

    /**
     * @param mediaType
     * @return The OutputFormat corresponding to the given media type, or null
     * if there is none.
     */
    public static OutputFormat getOutputFormat(String mediaType) {
        for (OutputFormat format : OutputFormat.values()) {
            if (format.getMediaType().equals(mediaType)) {
                return format;
            }
        }
        return null;
    }

    OutputFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    /**
     * Analog of {@link #equals} that also works with {@link SourceFormat}s.
     *
     * @param obj
     * @return Whether the instance is equal to the given object.
     */
    public boolean isEqual(Object obj) {
        if (obj instanceof SourceFormat) {
            return ((SourceFormat) obj).getExtensions().
                    contains(this.getExtension());
        }
        return this.equals(obj);
    }

    /**
     * @return Map serialization with <code>extension</code> and
     * <code>media_type</code> keys corresponding to string values.
     */
    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<>();
        map.put("extension", getExtension());
        map.put("media_type", getMediaType());
        return map;
    }

    /**
     * @return The extension.
     */
    public String toString() {
        return this.getExtension();
    }

}
