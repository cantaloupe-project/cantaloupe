package edu.illinois.library.cantaloupe.image;

/**
 * Normalized image metadata.
 */
public interface Metadata {

    /**
     * @return EXIF data, or {@literal null} if none is present. The data may
     *         be in a byte array or a {@link
     *         it.geosolutions.imageio.plugins.tiff.TIFFDirectory}.
     */
    Object getEXIF();

    /**
     * @return IPTC IIM data, or {@literal null} if none is present. The data
     *         may be in a byte array or a {@link
     *         it.geosolutions.imageio.plugins.tiff.TIFFDirectory}.
     */
    Object getIPTC();

    /**
     * @return Orientation of the image based on the EXIF {@literal
     *         Orientation} tag. If unknown or not specified, implementations
     *         should return {@link Orientation#ROTATE_0}.
     */
    Orientation getOrientation();

    /**
     * Returns an RDF/XML string in UTF-8 encoding. The root element is
     * {@literal rdf:RDF}, and there is no packet wrapper.
     *
     * @return XMP data packet, or {@literal null} if no XMP data is available.
     */
    String getXMP();

}
