package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/**
 * @see <a href="https://docs.oracle.com/javase/9/docs/api/javax/imageio/metadata/doc-files/gif_metadata.html">
 *      GIF Metadata Format Specification</a>
 */
public class GIFMetadata extends AbstractMetadata implements Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFMetadata.class);

    private boolean checkedForXMP;

    private boolean checkedFrameInterval;
    private int frameInterval;

    private boolean checkedLoopCount;
    private int loopCount = 1;

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
     * @return Frame interval of multi-frame (animated) GIFs, in milliseconds.
     *         If the GIF is malformed or not animated, {@literal 0} will be
     *         returned.
     */
    public int getFrameInterval() {
        if (!checkedFrameInterval) {
            checkedFrameInterval = true;
            // The node will be located at /GraphicControlExtension[@delayTime]
            // Basic testing indicates that tree traversal here is ~2x faster
            // than an XPath query.
            final NodeList graphicControlExtensionsList = getAsTree().
                    getElementsByTagName("GraphicControlExtension");
            if (graphicControlExtensionsList.getLength() > 0) {
                final IIOMetadataNode gcExtension =
                        (IIOMetadataNode) graphicControlExtensionsList.item(0);
                final NamedNodeMap attrs = gcExtension.getAttributes();
                final Node delayTimeAttr = attrs.getNamedItem("delayTime");
                if (delayTimeAttr != null) {
                    final String delayTimeStr = delayTimeAttr.getNodeValue();
                    if (delayTimeStr != null) {
                        frameInterval = Integer.parseInt(delayTimeStr) * 10;
                    }
                }
            }
        }
        return frameInterval;
    }

    /**
     * @return Null, as GIF does not support IPTC IIM.
     */
    @Override
    public Object getIPTC() {
        return null;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * Returns the loop count of multi-frame (animated) GIFs.
     *
     * @return If the GIF is malformed or not animated, {@literal 1} will be
     *         returned. If the loop is infinite, {@literal 0} will be
     *         returned.
     */
    public int getLoopCount() {
        if (!checkedLoopCount) {
            checkedLoopCount = true;
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
                    if ("NETSCAPE".equals(appIdAttr.getNodeValue())) {
                        byte[] b = (byte[]) appExtension.getUserObject();
                        if (b.length > 2) {
                            loopCount = b[2] & 0xFF | (b[1] & 0xFF) << 8;
                            break;
                        }
                    }
                }
            }
        }
        return loopCount;
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
        if (!checkedForXMP) {
            checkedForXMP = true;
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

}
