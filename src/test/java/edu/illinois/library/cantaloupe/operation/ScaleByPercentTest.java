package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ScaleByPercentTest extends ScaleTest {

    private ScaleByPercent instance;

    @Override
    ScaleByPercent newInstance() {
        ScaleByPercent instance = new ScaleByPercent();
        instance.setFilter(Scale.Filter.BOX);
        return instance;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Test
    void testNoOpConstructor() {
        assertEquals(1, instance.getPercent());
    }

    @Test
    void testDoubleConstructor() {
        instance = new ScaleByPercent(0.45);
        assertEquals(0.45, instance.getPercent());
    }

    @Test
    void testEqualsWithEqualInstances() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BOX);
        assertEquals(expected, instance);
    }

    @Test
    void testEqualsWithUnequalPercents() {
        ScaleByPercent expected = new ScaleByPercent(0.45);
        expected.setFilter(Scale.Filter.BOX);
        assertNotEquals(expected, instance);
    }

    @Test
    void testEqualsWithUnequalFilters() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BICUBIC);
        assertNotEquals(expected, instance);
    }

    @Test
    void testGetDifferentialScales() {
        final Dimension fullSize = new Dimension(300, 200);
        ReductionFactor rf       = new ReductionFactor(1);
        ScaleConstraint sc       = new ScaleConstraint(1, 4);

        instance = new ScaleByPercent(0.5);
        assertArrayEquals(new double[] { 0.25, 0.25 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);

        instance.setPercent(0.25);
        assertArrayEquals(new double[] { 0.125, 0.125 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);

        instance.setPercent(1.5);
        rf = new ReductionFactor();
        assertArrayEquals(new double[] { 0.375, 0.375 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void testGetReductionFactor() {
        Dimension size     = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance = new ScaleByPercent(0.45);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);

        instance.setPercent(0.2);
        assertEquals(2, instance.getReductionFactor(size, sc, 999).factor);
        assertEquals(1, instance.getReductionFactor(size, sc, 1).factor);
    }

    @Test
    void testGetResultingScales() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance = new ScaleByPercent(0.5);

        assertArrayEquals(new double[] { 0.5 * 1 / 3.0, 0.5 * 1 / 3.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingSize1WithDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new ScaleByPercent(0.5);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new ScaleByPercent(1.5);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize2WithPercent() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance = new ScaleByPercent();
        // down
        instance.setPercent(0.5);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
        // up
        instance.setPercent(1.5);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testHasEffect() {
        assertFalse(instance.hasEffect());
        instance = new ScaleByPercent(0.5);
        assertTrue(instance.hasEffect());
    }

    @Test
    void testHasEffectWithArguments() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList =
                new OperationList(new CropByPixels(0, 0, 300, 200));
        assertFalse(instance.hasEffect(fullSize, opList));

        instance.setPercent(0.5);
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void testHasEffectWithScaleConstraint() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList = new OperationList();
        opList.setScaleConstraint(new ScaleConstraint(1, 2));

        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void testHashCodeWithEqualInstances() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BOX);
        assertEquals(expected.hashCode(), instance.hashCode());
    }

    @Test
    void testHashCodeWithUnequalPercents() {
        ScaleByPercent expected = new ScaleByPercent(0.45);
        expected.setFilter(Scale.Filter.BOX);
        assertNotEquals(expected.hashCode(), instance.hashCode());
    }

    @Test
    void testHashCodeWithUnequalFilters() {
        ScaleByPercent expected = new ScaleByPercent(1);
        expected.setFilter(Scale.Filter.BICUBIC);
        assertNotEquals(expected.hashCode(), instance.hashCode());
    }

    @Test
    void testIsUp() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance.setPercent(0.5); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setPercent(1.0); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setPercent(1.2); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    void setPercent() {
        double percent = 0.5;
        instance.setPercent(percent);
        assertEquals(percent, instance.getPercent());
    }

    @Test
    void testSetPercentWithNegativePercent() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPercent(-0.5));
    }

    @Test
    void testSetPercentWithZeroPercent() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPercent(0.0));
    }

    @Test
    void testSetPercentWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setPercent(0.5));
    }

    @Test
    void testToMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Dimension resultingSize =
                instance.getResultingSize(fullSize, scaleConstraint);

        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(resultingSize.intWidth(), map.get("width"));
        assertEquals(resultingSize.intHeight(), map.get("height"));
    }

    @Test
    void testToString() {
        assertEquals("100%,box", instance.toString());
    }

}
