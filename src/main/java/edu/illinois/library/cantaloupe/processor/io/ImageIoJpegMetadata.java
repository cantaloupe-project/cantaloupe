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
    public Object getExif() {
        // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
        // identical nodes at /markerSequence/unknown[@MarkerTag=225]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                getElementsByTagName("markerSequence").item(0);
        final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                // Check the first byte to see whether it's EXIF or XMP.
                byte[] data = (byte[]) marker.getUserObject();
                if (data[0] == 69) { // TODO: is this right?
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
    public Object getIptc() {
        // IPTC metadata is located at /markerSequence/unknown[@MarkerTag=237]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                getElementsByTagName("markerSequence").item(0);
        NodeList unknowns = markerSequence.getElementsByTagName("unknown");
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
    public Object getXmp() {
        // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
        // identical nodes at /markerSequence/unknown[@MarkerTag=225]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                getElementsByTagName("markerSequence").item(0);
        final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                // Check the first byte to see whether it's EXIF or XMP.
                byte[] data = (byte[]) marker.getUserObject();
                if (data[0] == 104) { // TODO: is this right?
                    return data;
                }
            }
        }
        return null;
    }


}
