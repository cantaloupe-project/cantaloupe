package edu.illinois.library.cantaloupe.operation;

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
     * @return Map with a <code>type</code> key corresponding to the
     *         lowercase enum name.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
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
