package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.util.Map;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * should check instances' type and recast.
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
     * N.B. {@link #isNoOp(Dimension, OperationList)} is more reliable.
     *
     * @return Whether applying the operation on its own would result in an
     *         unmodified image.
     */
    boolean isNoOp();

    /**
     * Contextually-aware counterpart to {@link #isNoOp()}. For example, a
     * scale operation specifying a scale to 300x200, when the given operation
     * list contains a crop of 300x200, would return <code>true</code>.
     *
     * @param fullSize Full size of the source image.
     * @param opList Operation list of which the operation may or may not be a
     *               member.
     * @return Whether applying the operation in the context of the given
     *         full size and operation list would result in an unmodified
     *         image.
     */
    boolean isNoOp(Dimension fullSize, OperationList opList);

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map serialization of the operation that expresses the essence
     *         of the operation relative to the given full size. The map
     *         should include a string <code>operation</code> key pointing to
     *         the simple class name of the operation.
     */
    Map<String,Object> toMap(Dimension fullSize);

}
