package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

abstract class CropTest extends BaseTest {

    static final double DELTA = 0.0000001;

    abstract protected Crop newInstance();

    @Test
    void setOrientationThrowsExceptionWhenFrozen() {
        Crop instance = newInstance();
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setOrientation(Orientation.ROTATE_90));
    }

}
