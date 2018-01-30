package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.*;

public class RasterizationHelperTest extends BaseTest {

    private static final float FUDGE = 0.00001f;

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
        assertEquals(baseDPI, instance.getDPI(0), FUDGE);
    }

    @Test
    public void testGetDPIWithReductionFactor() {
        assertEquals(instance.getBaseDPI() * 2f, instance.getDPI(-1), FUDGE);
        assertEquals(instance.getBaseDPI(), instance.getDPI(0), FUDGE);
        assertEquals(instance.getBaseDPI() / 2f, instance.getDPI(1), FUDGE);
        assertEquals(instance.getBaseDPI() / 4f, instance.getDPI(2), FUDGE);
    }

    @Test
    public void testGetDPIWithHalfScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final Scale scale = new Scale(0.5f);
        assertEquals(instance.getBaseDPI() / 2f,
                instance.getDPI(scale, fullSize), FUDGE);
    }

    @Test
    public void testGetDPIWith1xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final Scale scale = new Scale(1);
        assertEquals(instance.getBaseDPI(),
                instance.getDPI(scale, fullSize), FUDGE);
    }

    @Test
    public void testGetDPIWith2xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final Scale scale = new Scale(2);
        assertEquals(instance.getBaseDPI() * 2f,
                instance.getDPI(scale, fullSize), FUDGE);
    }

    @Test
    public void testGetDPIWith3xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final Scale scale = new Scale(3);
        assertEquals(instance.getBaseDPI() * 3f,
                instance.getDPI(scale, fullSize), FUDGE);
    }

    @Test
    public void testGetDPIWith4xScale() {
        final Dimension fullSize = new Dimension(1000, 1000);
        final Scale scale = new Scale(4);
        assertEquals(instance.getBaseDPI() * 4f,
                instance.getDPI(scale, fullSize), FUDGE);
    }

}