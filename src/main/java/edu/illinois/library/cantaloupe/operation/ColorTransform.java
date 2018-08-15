package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.awt.Dimension;
import java.util.Collections;
import java.util.HashMap;
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
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("type", name().toLowerCase());
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
