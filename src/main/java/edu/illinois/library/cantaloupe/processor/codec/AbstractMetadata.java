package edu.illinois.library.cantaloupe.processor.codec;

import org.slf4j.Logger;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

abstract class AbstractMetadata {

    private String formatName;
    private IIOMetadata iioMetadata;

    AbstractMetadata(IIOMetadata metadata, String format) {
        iioMetadata = metadata;
        formatName = format;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIIOMetadata().getAsTree(getFormatName());
    }

    private String getFormatName() {
        return formatName;
    }

    IIOMetadata getIIOMetadata() {
        return iioMetadata;
    }

    abstract Logger getLogger();

    abstract String getXMP();

}
