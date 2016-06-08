package edu.illinois.library.cantaloupe.processor.io;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/**
 * Wraps an {@link IIOMetadata} instance for the purposes of ImageIO metadata
 * exchange, adding some convenient accessors to access specific metadata
 * types.
 */
public interface ImageIoMetadata {

    IIOMetadataNode getAsTree();

    /**
     * @return EXIF data, or null if none was found in the source metadata.
     */
    Object getExif();

    /**
     * @return TODO: what is this?
     */
    String getFormatName();

    /**
     * @return ImageIO metadata object.
     */
    IIOMetadata getIioMetadata();

    /**
     * @return IPTC data, or null if none was found in the source metadata.
     */
    Object getIptc();

    /**
     * @param metadata ImageIO metadata object.
     */
    void setIioMetadata(IIOMetadata metadata);

    /**
     * @param name TODO: what is this?
     */
    void setFormatName(String name);

    /**
     * @return XMP data, or null if none was found in the source metadata.
     */
    Object getXmp();

}
