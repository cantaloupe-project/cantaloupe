package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Map;

/**
 * Encapsulates a transposition (flipping/mirroring) operation on an image.
 */
public enum Transpose implements Operation {

    /** Indicates mirroring. */
    HORIZONTAL,
    /** Indicates flipping. */
    VERTICAL;

    /**
     * Does nothing.
     */
    @Override
    public void freeze() {
        // no-op
    }

    @Override
    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @return Map with an {@literal axis} key corresponding to the lowercase
     *         enum name.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "axis", name().toLowerCase());
    }

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to have any particular format.
     */
    @Override
    public String toString() {
        switch (this) {
            case HORIZONTAL:
                return "h";
            case VERTICAL:
                return "v";
            default:
                return super.toString();
        }
    }
}
