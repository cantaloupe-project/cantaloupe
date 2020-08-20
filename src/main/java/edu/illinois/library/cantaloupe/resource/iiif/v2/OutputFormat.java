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

    GIF("gif"),
    JP2("jp2"),
    JPG("jpg"),
    PDF("pdf"),
    PNG("png"),
    TIF("tif"),
    WEBP("webp");

    private final String formatKey;

    /**
     * @param formatKey {@link Format#getKey() Key} of an equivalent {@link
     *                  Format}.
     */
    OutputFormat(String formatKey) {
        this.formatKey = formatKey;
    }

    /**
     * @return Equivalent {@link Format}.
     */
    public Format toFormat() {
        return Format.get(formatKey);
    }

    @Override
    public String toString() {
        return toFormat().toString();
    }

}
