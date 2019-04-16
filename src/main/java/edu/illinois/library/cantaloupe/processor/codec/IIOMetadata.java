package edu.illinois.library.cantaloupe.processor.codec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.illinois.library.cantaloupe.image.Metadata;

import javax.imageio.metadata.IIOMetadataNode;

/**
 * Wraps a {@link javax.imageio.metadata.IIOMetadata}.
 */
public abstract class IIOMetadata extends Metadata {

    protected javax.imageio.metadata.IIOMetadata iioMetadata;
    private String formatName;

    protected IIOMetadata(javax.imageio.metadata.IIOMetadata iioMetadata,
                          String formatName) {
        this.iioMetadata = iioMetadata;
        this.formatName = formatName;
    }

    @JsonIgnore
    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) iioMetadata.getAsTree(formatName);
    }

}
