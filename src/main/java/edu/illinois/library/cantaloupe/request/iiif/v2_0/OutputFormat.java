package edu.illinois.library.cantaloupe.request.iiif.v2_0;

import edu.illinois.library.cantaloupe.image.SourceFormat;

/**
 * Encapsulates the "format" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#format">IIIF Image API 2.0</a>
 */
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
     * @param sourceFormat
     * @return Whether the instance is equal to the given source format.
     */
    public boolean isEqual(SourceFormat sourceFormat) {
        return sourceFormat.getExtensions().contains(this.getExtension());
    }

    public edu.illinois.library.cantaloupe.image.OutputFormat toOutputFormat() {
        return edu.illinois.library.cantaloupe.image.OutputFormat.valueOf(this.name());
    }

    /**
     * @return Value compatible with the format component of an IIIF URI.
     */
    public String toString() {
        return this.getExtension();
    }

}
