package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

class GifMetadata extends AbstractMetadata
        implements Metadata {

    /**
     * @param metadata
     * @param formatName
     */
    public GifMetadata(IIOMetadata metadata, String formatName) {
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

    @Override
    public Orientation getOrientation() {
        final String xmpData = new String(getXmp());
        // Trim off the junk
        final int start = xmpData.indexOf("<rdf:RDF");
        final int end = xmpData.indexOf("</rdf:RDF");
        final String xmp = xmpData.substring(start, end + 10);

        final Orientation orientation = readOrientation(xmp);
        if (orientation != null) {
            return orientation;
        }
        return Orientation.ROTATE_0;
    }

    /**
     * @return
     * @see <a href="http://partners.adobe.com/public/developer/en/xmp/sdk/XMPspecification.pdf">
     *      Embedding XMP Metadata in Application Files</a>
     */
    @Override
    public byte[] getXmp() {
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
                            byte[] xmp = (byte[]) appExtension.getUserObject();
                            // Ideally we would just be able to return the byte
                            // array here, but testing indicates that the XMP
                            // data is corrupt in some/all(?) cases, even when
                            // it is valid in the source file. (?)
                            // TODO: this is horrible
                            String xmpStr = new String(xmp);
                            xmpStr = StringUtils.replace(xmpStr, "xmpmta", "xmpmeta");
                            xmpStr = StringUtils.replace(xmpStr, "\n/", "\n</");
                            return xmpStr.getBytes();
                        }
                    }
                }
            }
        }
        return null;
    }

}
