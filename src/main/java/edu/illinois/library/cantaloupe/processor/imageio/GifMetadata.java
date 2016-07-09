package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

class GifMetadata extends AbstractMetadata implements Metadata {

    private boolean checkedForXmp = false;

    /** Cached by getOrientation() */
    private Orientation orientation;

    /** Cached by getXmp() */
    private String xmp;

    /**
     * @param metadata
     * @param formatName
     */
    GifMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null, as GIF does not support EXIF.
     */
    @Override
    public Object getExif() {
        return null;
    }

    /**
     * @return Null, as GIF does not support IPTC IIM.
     */
    @Override
    public Object getIptc() {
        return null;
    }

    /**
     * @return Effective orientation of the image. The return value is cached.
     */
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            String xmp = getXmp();
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
     * @return
     * @see <a href="http://partners.adobe.com/public/developer/en/xmp/sdk/XMPspecification.pdf">
     *      Embedding XMP Metadata in Application Files</a>
     */
    @Override
    public String getXmp() {
        if (!checkedForXmp) {
            checkedForXmp = true;
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
                                byte[] xmpData = (byte[]) appExtension.getUserObject();
                                xmp = new String(xmpData);

                                // Testing indicates that the XMP data is
                                // corrupt in some/all(?) cases, even when it
                                // is valid in the source file. (?)
                                // TODO: this is horrible and probably insufficient
                                xmp = StringUtils.replace(xmp, "xmpmta", "xmpmeta");
                                xmp = StringUtils.replace(xmp, "\n/", "\n</");

                                // Trim off the junk
                                final int start = xmp.indexOf("<rdf:RDF");
                                final int end = xmp.indexOf("</rdf:RDF");
                                xmp = xmp.substring(start, end + 10);
                            }
                        }
                    }
                }
            }
        }
        return xmp;
    }

}
