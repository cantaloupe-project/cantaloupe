package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

public interface Metadata {

    /**
     * @return Metadata as an ImageIO DOM tree, or {@literal null} if the
     *         instance is not backed by an {@link IIOMetadata}.
     */
    IIOMetadataNode getAsTree();

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
     * @return XMP data packet, or {@literal null} if none is available.
     */
    byte[] getXMP();

}
