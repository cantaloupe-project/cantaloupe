package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

class MockOverlay extends Overlay {

    MockOverlay() {
        super(Position.TOP_LEFT, 0);
    }

    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        return new HashMap<>();
    }

}
