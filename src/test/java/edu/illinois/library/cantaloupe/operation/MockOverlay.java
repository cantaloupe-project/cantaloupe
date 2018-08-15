package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

public class MockOverlay extends Overlay {

    public MockOverlay() {
        super(Position.TOP_LEFT, 0);
    }

    @Override
    public Map<String, Object> toMap(Dimension fullSize,
                                     ScaleConstraint scaleConstraint) {
        return new HashMap<>();
    }

}
