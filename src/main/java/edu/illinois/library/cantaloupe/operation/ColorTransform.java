package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Map;

/**
 * Encapsulates a color transform operation.
 */
public enum ColorTransform implements Operation {

    BITONAL, GRAY;

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
     * @return Map with a {@literal type} key corresponding to the lowercase
     *         enum name.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        return Map.of(
                "class", getClass().getSimpleName(),
                "type", name().toLowerCase());
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
