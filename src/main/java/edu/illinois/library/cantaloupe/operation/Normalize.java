package edu.illinois.library.cantaloupe.operation;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates an auto-contrast-stretching operation.
 *
 * @since 3.4
 */
public class Normalize implements Operation {

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

    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        return map;
    }

    /**
     * @return The string <code>normalize</code>.
     */
    @Override
    public String toString() {
        return "normalize";
    }

}
