package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * (e.g. {@link edu.illinois.library.cantaloupe.processor.Processor}s) should
 * check instances' type and recast.
 */
public interface Operation {

    /**
     * @param fullSize
     * @return Resulting dimensions when the operation is applied to an image
     * of the given full size.
     */
    Dimension getResultingSize(Dimension fullSize);

    /**
     * @return Whether an application of the operation would result in an
     * identical source image.
     */
    boolean isNoOp();

}
