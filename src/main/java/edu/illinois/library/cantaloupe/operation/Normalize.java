package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.awt.Dimension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates an auto-contrast-stretching operation.
 *
 * @since 3.4
 */
public class Normalize implements Operation {

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

    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return The string {@literal normalize}.
     */
    @Override
    public String toString() {
        return "normalize";
    }

}
