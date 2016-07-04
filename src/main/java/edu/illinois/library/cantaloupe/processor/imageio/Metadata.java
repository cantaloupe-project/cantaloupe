package edu.illinois.library.cantaloupe.processor.imageio;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/**
 * Wraps an {@link IIOMetadata} instance for the purposes of ImageIO metadata
 * exchange, adding some convenient accessors to access specific metadata
 * types and properties.
 */
public interface Metadata {

    enum Orientation {
        ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270
    }

    /**
     * @return Metadata as an ImageIO DOM tree.
     */
    IIOMetadataNode getAsTree();

    /**
     * @return EXIF data, or null if none was found in the source metadata.
     */
    Object getExif();

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
     * @return XMP data, or null if none was found in the source metadata.
     */
    Object getXmp();

}
