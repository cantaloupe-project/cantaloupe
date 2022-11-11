package edu.illinois.library.cantaloupe.processor.codec.jpeg2000;

import edu.illinois.library.cantaloupe.processor.codec.IIOMetadata;

class JPEG2000Metadata extends IIOMetadata {
    protected JPEG2000Metadata(javax.imageio.metadata.IIOMetadata iioMetadata) {
        super(iioMetadata, "jp2");
    }
}
