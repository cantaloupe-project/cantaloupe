package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

abstract class CropTest extends BaseTest {

    static final double DELTA = 0.0000001;

    abstract protected Crop newInstance();

    @Test(expected = IllegalStateException.class)
    public void setOrientationThrowsExceptionWhenFrozen() {
        Crop instance = newInstance();
        instance.freeze();
        instance.setOrientation(Orientation.ROTATE_90);
    }

}
