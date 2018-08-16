package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RasterizationHelperTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private RasterizationHelper instance;

    @Before
    public void setUp() {
        instance = new RasterizationHelper();
        instance.setBaseDPI(150);
    }

    @Test
    public void testConstructor() {
        final int baseDPI = 200;
        Configuration.getInstance().setProperty(Key.PROCESSOR_DPI, baseDPI);
        instance = new RasterizationHelper();
    }

    @Test
    public void testGetDPIWithHalfScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Scale scale = new Scale(0.5f);
        assertEquals(instance.getBaseDPI() / 2.0,
                instance.getDPI(scale, fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void testGetDPIWith1xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Scale scale = new Scale(1);
        assertEquals(instance.getBaseDPI(),
                instance.getDPI(scale, fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void testGetDPIWith2xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Scale scale = new Scale(2);
        assertEquals(instance.getBaseDPI() * 2,
                instance.getDPI(scale, fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void testGetDPIWith3xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Scale scale = new Scale(3);
        assertEquals(instance.getBaseDPI() * 3,
                instance.getDPI(scale, fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void testGetDPIWith4xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Scale scale = new Scale(4);
        assertEquals(instance.getBaseDPI() * 4,
                instance.getDPI(scale, fullSize, scaleConstraint), DELTA);
    }

}