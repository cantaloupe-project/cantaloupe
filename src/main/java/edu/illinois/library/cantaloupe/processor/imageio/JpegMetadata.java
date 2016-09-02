package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Orientation;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/**
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html">
 *      JPEG Metadata Format Specification and Usage Notes</a>
 */
class JpegMetadata extends AbstractMetadata implements Metadata {

    private boolean checkedForExif = false;
    private boolean checkedForIptc = false;
    private boolean checkedForXmp = false;

    /** Cached by getExif() */
    private byte[] exif;

    /** Cached by getIptc() */
    private byte[] iptc;

    /** Cached by getOrientation() */
    private Orientation orientation;

    /** Cached by getXmp() */
    private byte[] xmp;

    /**
     * @param metadata
     * @param formatName
     */
    JpegMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return EXIF data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getExif() {
        if (!checkedForExif) {
            checkedForExif = true;
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
                        exif = data;
                    }
                }
            }
        }
        return exif;
    }

    /**
     * @return Orientation from the non-XMP EXIF metadata. May be null.
     */
    Orientation getExifOrientation() {
        return readOrientation(getExif());
    }

    /**
     * @return IPTC data, or null if none was found in the source metadata.
     */
    @Override
    public byte[] getIptc() {
        if (!checkedForIptc) {
            checkedForIptc = true;
            // IPTC metadata appears in the IIOMetadataNode tree at
            // /markerSequence/unknown[@MarkerTag=237]
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("237".equals(marker.getAttribute("MarkerTag"))) {
                    iptc = (byte[]) marker.getUserObject();
                }
            }
        }
        return iptc;
    }

    /**
     * @return Orientation from the metadata. EXIF is checked first, then XMP.
     *         If not found, {@link Orientation#ROTATE_0} will be returned.
     */
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            // Check EXIF.
            orientation = getExifOrientation();
            if (orientation == null) {
                // Check XMP.
                final String xmp = getXmpRdf();
                if (xmp != null) {
                    orientation = getXmpOrientation();
                }
            }
            if (orientation == null) {
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    @Override
    public byte[] getXmp() {
        if (!checkedForXmp) {
            checkedForXmp = true;
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
                        xmp = data;
                    }
                }
            }
        }
        return xmp;
    }

    /**
     * @return Orientation from the XMP metadata. May be null.
     */
    Orientation getXmpOrientation() {
        Orientation orientation = null;
        final String xmp = getXmpRdf();
        if (xmp != null) {
            orientation = readOrientation(xmp);
        }
        return orientation;
    }

    @Override
    public String getXmpRdf() {
        final byte[] xmpData = getXmp();
        if (xmpData != null) {
            final String xmp = new String(xmpData);
            // Trim off the junk
            final int start = xmp.indexOf("<rdf:RDF");
            final int end = xmp.indexOf("</rdf:RDF");
            return xmp.substring(start, end + 10);
        }
        return null;
    }

}
