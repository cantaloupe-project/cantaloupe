package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/**
 * Wraps an {@link IIOMetadata} instance for the purposes of ImageIO metadata
 * exchange, adding some convenient accessors to access specific metadata
 * types and properties.
 */
public interface Metadata {

    /**
     * @return Metadata as an ImageIO DOM tree.
     */
    IIOMetadataNode getAsTree();

    /**
     * @return EXIF data, or null if none was found in the source metadata.
     */
    Object getExif();

    IIOMetadata getIioMetadata();

    /**
     * @return IPTC data, or null if none was found in the source metadata.
     */
    Object getIptc();

    /**
     * @return Orientation of the image based on the EXIF "Orientation" tag.
     *         If unknown or not specified, implementations should return
     *         {@link Orientation#ROTATE_0}.
     */
    Orientation getOrientation();

    /**
     * @return XMP data packet, or null if none was found in the source
     *         metadata.
     */
    byte[] getXmp();

    /**
     * @return XMP RDF/XML string, or null if none was found in the source
     *         metadata.
     */
    String getXmpRdf();

}
