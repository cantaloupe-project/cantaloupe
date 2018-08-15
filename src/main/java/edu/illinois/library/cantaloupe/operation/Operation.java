package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.awt.Dimension;
import java.util.Map;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * should check instances' type and recast.
 */
public interface Operation {

    /**
     * Freezes the instance, making it immutable. When frozen, mutation methods
     * should throw an {@link IllegalStateException} and getters should return
     * immutable values, if possible. (But they should do that anyway.)
     */
    void freeze();

    /**
     * This default implementation returns the {@literal fullSize} argument.
     * It will need to be overridden by operations that could change the
     * image's resulting size.
     *
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint applied to the given full size.
     * @return                Resulting dimensions when the operation is
     *                        applied.
     */
    default Dimension getResultingSize(Dimension fullSize,
                                       ScaleConstraint scaleConstraint) {
        return fullSize;
    }

    /**
     * Simpler but less-accurate counterpart of
     * {@link #hasEffect(Dimension, OperationList)}.
     *
     * @return Whether applying the operation on its own would result in a
     *         changed image.
     */
    boolean hasEffect();

    /**
     * Context-aware counterpart to {@link #hasEffect()}. For example, a scale
     * operation specifying a scale to 300&times;200, when the given operation
     * list contains a crop of 300&times;200, would return {@literal false}.
     *
     * @param fullSize Full size of the source image.
     * @param opList   Operation list of which the operation may or may not be
     *                 a member.
     * @return         Whether applying the operation in the context of the
     *                 given full size and operation list would result in a
     *                 changed image.
     */
    boolean hasEffect(Dimension fullSize, OperationList opList);

    /**
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint applied to the given full size.
     * @return                Unmodifiable Map serialization of the operation
     *                        that expresses the essence of the operation
     *                        relative to the given arguments. The map includes
     *                        a string {@literal class} key pointing to the
     *                        operation's simple class name.
     */
    Map<String,Object> toMap(Dimension fullSize,
                             ScaleConstraint scaleConstraint);

    /**
     * <p>Validates the instance, throwing an exception if invalid.</p>
     *
     * <p>Implementations can also throw exceptions from property setters as an
     * alternative to using this method.</p>
     *
     * <p>This default implementation does nothing.</p>
     *
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint applied to the given full size.
     * @throws ValidationException if the instance is invalid.
     */
    default void validate(Dimension fullSize,
                          ScaleConstraint scaleConstraint) throws ValidationException {}

}
