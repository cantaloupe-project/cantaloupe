package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.util.Map;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * (e.g. {@link edu.illinois.library.cantaloupe.processor.Processor}s) should
 * check instances' type and recast.
 */
public interface Operation {

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Resulting dimensions when the operation is applied to an image
     *         of the given full size.
     */
    Dimension getResultingSize(Dimension fullSize);

    /**
     * @return Whether an application of the operation would result in an
     * identical source image.
     */
    boolean isNoOp();

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map serialization of the operation that expresses the essence
     *         of the operation relative to the given full size.
     */
    Map<String,Object> toMap(Dimension fullSize);

}
