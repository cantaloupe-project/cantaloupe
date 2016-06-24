package edu.illinois.library.cantaloupe.processor.io;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

class AbstractImageIoMetadata {

    private String formatName;
    private IIOMetadata iioMetadata;

    AbstractImageIoMetadata(IIOMetadata metadata, String format) {
        iioMetadata = metadata;
        formatName = format;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIioMetadata().getAsTree(getFormatName());
    }

    private String getFormatName() {
        return formatName;
    }

    IIOMetadata getIioMetadata() {
        return iioMetadata;
    }

}
