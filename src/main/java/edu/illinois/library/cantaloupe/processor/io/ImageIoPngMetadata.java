package edu.illinois.library.cantaloupe.processor.io;

import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

class ImageIoPngMetadata extends AbstractImageIoMetadata
        implements ImageIoMetadata {

    /**
     * @param metadata
     * @param formatName
     */
    public ImageIoPngMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null.
     */
    @Override
    public Object getExif() {
        return null;
    }

    /**
     * @return Null.
     */
    @Override
    public Object getIptc() {
        return null;
    }

    @Override
    public Object getXmp() {
        final NodeList itxtNodes = getAsTree().getElementsByTagName("iTXt");
        for (int i = 0; i < itxtNodes.getLength(); i++) {
            final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
            final NodeList entries = itxtNode.getElementsByTagName("iTXtEntry");
            for (int j = 0; j < entries.getLength(); j++) {
                final String keyword = ((IIOMetadataNode) entries.item(j)).
                        getAttribute("keyword");
                if ("XML:com.adobe.xmp".equals(keyword)) {
                    return ((IIOMetadataNode) entries.item(j)).
                            getAttribute("text");
                }
            }
        }
        return null;
    }

}
