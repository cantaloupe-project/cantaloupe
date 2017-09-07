package edu.illinois.library.cantaloupe.operation;

import java.awt.Dimension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a transposition (flipping/mirroring) operation on an image.
 */
public enum Transpose implements Operation {

    /** Indicates mirroring. */
    HORIZONTAL,
    /** Indicates flipping. */
    VERTICAL;

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
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
     * @param fullSize Ignored.
     * @return Map with an <code>axis</code> key corresponding to the
     *         lowercase enum name.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("axis", this.name().toLowerCase());
        return Collections.unmodifiableMap(map);
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
