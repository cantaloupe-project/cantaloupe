package edu.illinois.library.cantaloupe.request;

public enum Format {

    GIF("gif", "request/gif"),
    JP2("jp2", "request/jp2"),
    JPG("jpg", "request/jpeg"),
    PDF("pdf", "application/pdf"),
    PNG("png", "request/png"),
    TIF("tif", "request/tiff"),
    WEBP("webp", "request/webp");

    private String extension;
    private String mediaType;

    Format(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getMediaType() {
        return this.mediaType;
    }

}
