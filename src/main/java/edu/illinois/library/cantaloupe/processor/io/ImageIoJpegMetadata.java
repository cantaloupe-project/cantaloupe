package edu.illinois.library.cantaloupe.processor.io;

import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

class ImageIoJpegMetadata extends AbstractImageIoMetadata
        implements ImageIoMetadata {

    /**
     * @param metadata
     * @param formatName
     */
    public ImageIoJpegMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return EXIF data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getExif() {
        // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
        // identical nodes at /markerSequence/unknown[@MarkerTag=225]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                getElementsByTagName("markerSequence").item(0);
        final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                byte[] data = (byte[]) marker.getUserObject();
                // Check the first byte to see whether it's EXIF or XMP.
                if (data[0] == 69) {
                    return data;
                }
            }
        }
        return null;
    }

    /**
     * @return IPTC data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getIptc() {
        // IPTC metadata appears in the IIOMetadataNode tree at
        // /markerSequence/unknown[@MarkerTag=237]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                getElementsByTagName("markerSequence").item(0);
        final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("237".equals(marker.getAttribute("MarkerTag"))) {
                return (byte[]) marker.getUserObject();
            }
        }
        return null;
    }

    /**
     * @return XMP data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getXmp() {
        // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
        // identical nodes at /markerSequence/unknown[@MarkerTag=225]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                getElementsByTagName("markerSequence").item(0);
        final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                byte[] data = (byte[]) marker.getUserObject();
                // Check the first byte to see whether it's EXIF or XMP.
                if (data[0] == 104) {
                    return data;
                }
            }
        }
        return null;
    }


}
