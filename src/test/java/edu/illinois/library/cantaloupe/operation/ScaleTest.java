package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilters;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

abstract class ScaleTest extends BaseTest {

    @Nested
    class FilterTest extends BaseTest {

        @Test
        void testToResampleFilter() {
            assertSame(ResampleFilters.getBellFilter(),
                    Scale.Filter.BELL.toResampleFilter());
            assertSame(ResampleFilters.getBiCubicFilter(),
                    Scale.Filter.BICUBIC.toResampleFilter());
            assertSame(ResampleFilters.getBoxFilter(),
                    Scale.Filter.BOX.toResampleFilter());
            assertSame(ResampleFilters.getBSplineFilter(),
                    Scale.Filter.BSPLINE.toResampleFilter());
            assertSame(ResampleFilters.getHermiteFilter(),
                    Scale.Filter.HERMITE.toResampleFilter());
            assertSame(ResampleFilters.getLanczos3Filter(),
                    Scale.Filter.LANCZOS3.toResampleFilter());
            assertSame(ResampleFilters.getMitchellFilter(),
                    Scale.Filter.MITCHELL.toResampleFilter());
            assertSame(ResampleFilters.getTriangleFilter(),
                    Scale.Filter.TRIANGLE.toResampleFilter());
        }

    }

    static final double DELTA = 0.00000001;

    abstract Scale newInstance();

    @Test
    void testSetFilterWhenFrozenThrowsException() {
        Scale instance = newInstance();
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setFilter(Scale.Filter.LANCZOS3));
    }

    @Test
    void testToMapReturnsUnmodifiableMap() {
        Dimension fullSize              = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = newInstance().toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

}
