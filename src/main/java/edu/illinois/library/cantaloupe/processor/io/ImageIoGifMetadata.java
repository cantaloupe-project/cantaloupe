package edu.illinois.library.cantaloupe.processor.io;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

class ImageIoGifMetadata extends AbstractImageIoMetadata
        implements ImageIoMetadata {

    /**
     * @param metadata
     * @param formatName
     */
    public ImageIoGifMetadata(IIOMetadata metadata, String formatName) {
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
     * @return
     * @see <a href="http://xml.coverpages.org/XMP-Embedding.pdf">Embedding
     *      XMP Metadata in Application Files</a>
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
                            // it is valid in the source file. Maybe an ImageIO
                            // bug?
                            // These are probably not the only fixes that will
                            // need to be made.
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
