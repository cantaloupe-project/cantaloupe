package edu.illinois.library.cantaloupe.processor.codec.generic;

import edu.illinois.library.cantaloupe.processor.codec.IIOMetadata;

public class GenericMetadata extends IIOMetadata {

    /**
     * Creates an instance with conventional accessors.
     */
    public GenericMetadata() {
        this(null, null);
    }

    /**
     * Creates an instance whose getters ignore the setters and read from the
     * supplied arguments instead.
     */
    public GenericMetadata(javax.imageio.metadata.IIOMetadata metadata,
                           String formatName) {
        super(metadata, formatName);
    }

}
