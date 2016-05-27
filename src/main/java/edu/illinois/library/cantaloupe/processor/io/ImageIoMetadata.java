package edu.illinois.library.cantaloupe.processor.io;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadata;

/**
 * Wraps an {@link IIOMetadata} instance for the purposes of ImageIO metadata
 * exchange.
 */
public class ImageIoMetadata {

    private String formatName;
    private IIOMetadata iioMetadata;

    public ImageIoMetadata(IIOMetadata metadata, String formatName) {
        setIioMetadata(metadata);
        setFormatName(formatName);
    }

    public Node getAsTree() {
        return getIioMetadata().getAsTree(getFormatName());
    }

    public String getFormatName() {
        return formatName;
    }

    public IIOMetadata getIioMetadata() {
        return iioMetadata;
    }

    public void setIioMetadata(IIOMetadata metadata) {
        iioMetadata = metadata;
    }

    public void setFormatName(String name) {
        formatName = name;
    }

}
