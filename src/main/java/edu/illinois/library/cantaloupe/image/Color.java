package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a color transform operation.
 */
public enum Color implements Operation {

    BITONAL, GRAY;

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    @Override
    public boolean isNoOp() {
        return false;
    }

    @Override
    public boolean isNoOp(Dimension fullSize, OperationList opList) {
        return isNoOp();
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
        map.put("type", this.name().toLowerCase());
        return map;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
