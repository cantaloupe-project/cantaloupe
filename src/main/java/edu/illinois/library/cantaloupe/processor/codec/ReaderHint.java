package edu.illinois.library.cantaloupe.processor.codec;

public enum ReaderHint {

    /**
     * Returned from a reader. The reader has read only the requested region
     * of the image and there will be no need to crop it any further.
     */
    ALREADY_CROPPED,

    /**
     * Returned from a reader. The reader has already transformed the image
     * according to its embedded orientation property.
     */
    ALREADY_ORIENTED,

    /**
     * Provided to a reader, telling it to read the entire image ignoring
     * {@link edu.illinois.library.cantaloupe.operation.Crop} operations.
     */
    IGNORE_CROP

}
