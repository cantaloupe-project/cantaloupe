package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.UnsupportedEncodingException;

/**
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/gif_metadata.html">
 *      GIF Metadata Format Specification</a>
 */
class GIFMetadata extends AbstractMetadata implements Metadata {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(GIFMetadata.class);

    private boolean checkedForXmp = false;

    /** Cached by getOrientation() */
    private Orientation orientation;

    /** Cached by getXMP() */
    private byte[] xmp;

    /**
     * @param metadata
     * @param formatName
     */
    GIFMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null, as GIF does not support EXIF.
     */
    @Override
    public Object getEXIF() {
        return null;
    }

    /**
     * @return Null, as GIF does not support IPTC IIM.
     */
    @Override
    public Object getIPTC() {
        return null;
    }

    /**
     * @return Effective orientation of the image. The return value is cached.
     */
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            String xmp = getXMPRDF();
            if (xmp != null) {
                orientation = readOrientation(xmp);
            }
            if (orientation == null) {
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    /**
     * @return XMP RDF/XML string.
     * @see <a href="https://www.adobe.com/content/dam/Adobe/en/devnet/xmp/pdfs/XMPSpecificationPart3.pdf">
     *      XMP Specification Part 3</a>
     * @see <a href="http://partners.adobe.com/public/developer/en/xmp/sdk/XMPspecification.pdf">
     *      Embedding XMP Metadata in Application Files</a>
     */
    @Override
    public byte[] getXMP() {
        if (!checkedForXmp) {
            checkedForXmp = true;
            /*
            Testing indicates that the XMP data, as returned by the reader, is
            corrupt in pretty much all cases, even when it appears to be
            valid in the source file. It could be an ImageIO GIF plugin bug
            (it wouldn't be the only one) or something I'm doing wrong. In any
            case, the effort/reward ratio doesn't justify looking into it any
            further at this time, as probably no one is serving GIF source
            images anyway.

            // The XMP node will be located at /ApplicationExtensions/
            // ApplicationExtension[@applicationID="XMP Data" @authenticationCode="XMP"]
            final NodeList appExtensionsList = getAsTree().
                    getElementsByTagName("ApplicationExtensions");
            if (appExtensionsList.getLength() > 0) {
                final IIOMetadataNode appExtensions =
                        (IIOMetadataNode) appExtensionsList.item(0);
                final NodeList appExtensionList = appExtensions.
                        getElementsByTagName("ApplicationExtension");
                for (int i = 0; i < appExtensionList.getLength(); i++) {
                    final IIOMetadataNode appExtension =
                            (IIOMetadataNode) appExtensionList.item(i);
                    final NamedNodeMap attrs = appExtension.getAttributes();
                    final Node appIdAttr = attrs.getNamedItem("applicationID");
                    if (appIdAttr != null) {
                        final String appId = appIdAttr.getNodeValue();
                        if (appId != null) {
                            if ("xmp data".equals(appId.toLowerCase())) {
                                // TODO: this data is corrupt
                                xmp = (byte[]) appExtension.getUserObject();
                            }
                        }
                    }
                }
            } */
        }
        return xmp;
    }

    @Override
    public String getXMPRDF() {
        final byte[] xmpData = getXMP();
        if (xmpData != null) {
            try {
                final String xmp = new String(xmpData, "UTF-8");
                // Trim off the junk
                final int start = xmp.indexOf("<rdf:RDF");
                final int end = xmp.indexOf("</rdf:RDF");
                return xmp.substring(start, end + 10);
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("getXMPRDF(): {}", e.getMessage());
            }
        }
        return null;
    }

}
