package edu.illinois.library.cantaloupe.processor.io;

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
                            return (byte[]) appExtension.getUserObject();
                        }
                    }
                }
            }
        }
        return null;
    }

}
