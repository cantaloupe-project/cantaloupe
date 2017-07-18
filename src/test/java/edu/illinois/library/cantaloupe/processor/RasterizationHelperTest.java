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

    private static final int DEFAULT_DPI = 150;
    private static final float FUDGE = 0.00001f;

    private RasterizationHelper instance;

    @Before
    public void setUp() {
        instance = new RasterizationHelper();

        Configuration.getInstance().setProperty(Key.PROCESSOR_DPI, DEFAULT_DPI);
    }

    @Test
    public void testGetDPIWithReductionFactor() {
        assertEquals(DEFAULT_DPI * 2f, instance.getDPI(-1), FUDGE);
        assertEquals(DEFAULT_DPI, instance.getDPI(0), FUDGE);
        assertEquals(DEFAULT_DPI / 2f, instance.getDPI(1), FUDGE);
        assertEquals(DEFAULT_DPI / 4f, instance.getDPI(2), FUDGE);
    }

    @Test
    public void testGetDPIWithScale() {
        final Dimension fullSize = new Dimension(1000, 1000);

        Scale scale = new Scale(0.5f);
        assertEquals(DEFAULT_DPI / 2f, instance.getDPI(scale, fullSize), FUDGE);

        scale = new Scale(1);
        assertEquals(DEFAULT_DPI, instance.getDPI(scale, fullSize), FUDGE);

        scale = new Scale(2);
        assertEquals(DEFAULT_DPI * 2f, instance.getDPI(scale, fullSize), FUDGE);

        scale = new Scale(3);
        assertEquals(DEFAULT_DPI * 2f, instance.getDPI(scale, fullSize), FUDGE);

        scale = new Scale(4);
        assertEquals(DEFAULT_DPI * 4f, instance.getDPI(scale, fullSize), FUDGE);
    }

}