package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Format;

/**
 * Legal Image API output format. This is used only within the context of
 * Image API request handling; {@link Format} is generally used elsewhere.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#format">IIIF Image API 2.1:
 * Format</a>
 */
public enum OutputFormat {

    GIF(Format.GIF),
    JP2(Format.JP2),
    JPG(Format.JPG),
    PDF(Format.PDF),
    PNG(Format.PNG),
    TIF(Format.TIF),
    WEBP(Format.WEBP);

    private Format format;

    OutputFormat(Format equivalentFormat) {
        this.format = equivalentFormat;
    }

    /**
     * @return Equivalent {@link Format}.
     */
    public Format toFormat() {
        return format;
    }

    @Override
    public String toString() {
        return toFormat().toString();
    }

}
