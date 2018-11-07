package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Metadata;

import javax.imageio.metadata.IIOMetadataNode;

/**
 * Wraps a {@link javax.imageio.metadata.IIOMetadata}.
 */
abstract class IIOMetadata extends Metadata {

    private javax.imageio.metadata.IIOMetadata iioMetadata;
    private String formatName;

    IIOMetadata(javax.imageio.metadata.IIOMetadata iioMetadata,
                String formatName) {
        this.iioMetadata = iioMetadata;
        this.formatName = formatName;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIIOMetadata().getAsTree(formatName);
    }

    javax.imageio.metadata.IIOMetadata getIIOMetadata() {
        return iioMetadata;
    }

}
