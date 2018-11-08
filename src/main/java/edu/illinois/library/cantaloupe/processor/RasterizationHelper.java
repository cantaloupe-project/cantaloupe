package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;

import java.util.Arrays;

/**
 * Assists in rasterization of vector source images.
 */
class RasterizationHelper {

    private static final int FALLBACK_DPI = 150;

    private int baseDPI;

    RasterizationHelper() {
        baseDPI = Configuration.getInstance().
                getInt(Key.PROCESSOR_DPI, FALLBACK_DPI);
    }

    /**
     * @return DPI at 1x scale.
     */
    int getBaseDPI() {
        return baseDPI;
    }

    /**
     * @return DPI appropriate for the given reduction factor.
     */
    double getDPI(int reductionFactor,
                  final ScaleConstraint scaleConstraint) {
        double rfDPI = baseDPI;
        // Decrease the DPI if the reduction factor is positive.
        for (int i = 0; i < reductionFactor; i++) {
            rfDPI /= 2.0;
        }
        // Increase the DPI if the reduction factor is negative.
        for (int i = 0; i > reductionFactor; i--) {
            rfDPI *= 2.0;
        }

        double scDPI = baseDPI * scaleConstraint.getRational().doubleValue();

        return Math.min(rfDPI, scDPI);
    }

    double getDPI(final Scale scale,
                  final Dimension fullSize,
                  final ScaleConstraint scaleConstraint) {
        double[] scales = scale.getResultingScales(fullSize, scaleConstraint);
        double minScale = Arrays.stream(scales).min().orElse(1);
        return baseDPI * minScale;
    }

    /**
     * @param dpi DPI at 1x scale.
     */
    void setBaseDPI(int dpi) {
        this.baseDPI = dpi;
    }

}
