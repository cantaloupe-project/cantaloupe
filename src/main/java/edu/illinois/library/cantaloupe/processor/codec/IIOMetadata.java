package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Metadata;
import org.slf4j.Logger;

import javax.imageio.metadata.IIOMetadataNode;

abstract class IIOMetadata extends Metadata {

    private String formatName;
    private javax.imageio.metadata.IIOMetadata iioMetadata;

    IIOMetadata(javax.imageio.metadata.IIOMetadata metadata, String format) {
        iioMetadata = metadata;
        formatName = format;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIIOMetadata().getAsTree(getFormatName());
    }

    private String getFormatName() {
        return formatName;
    }

    javax.imageio.metadata.IIOMetadata getIIOMetadata() {
        return iioMetadata;
    }

    abstract Logger getLogger();

}
