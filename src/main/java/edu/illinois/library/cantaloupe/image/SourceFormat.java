package edu.illinois.library.cantaloupe.image;

public enum SourceFormat {

    BMP("bmp", "image/bmp"),
    GIF("gif", "image/gif"),
    JP2("jp2", "image/jp2"),
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    TIF("tif", "image/tiff"),
    WEBP("webp", "image/webp"),
    UNKNOWN("unknown", "unknown/unknown");

    private String extension;
    private String mediaType;

    SourceFormat(String extension, String mediaType) {
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
     * @return Extension.
     */
    public String toString() {
        return this.getExtension();
    }

}
