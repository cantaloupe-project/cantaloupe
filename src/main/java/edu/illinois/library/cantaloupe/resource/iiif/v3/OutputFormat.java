package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.image.Format;

/**
 * Legal Image API output format. This is used only within the context of
 * Image API request handling; {@link Format} is generally used elsewhere.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#45-format">IIIF Image API 3.0:
 * Format</a>
 */
enum OutputFormat {

    GIF(Format.GIF),
    JP2(Format.JP2),
    JPG(Format.JPG),
    PDF(Format.PDF),
    PNG(Format.PNG),
    TIF(Format.TIF),
    WEBP(Format.WEBP);

    private final Format format;

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
