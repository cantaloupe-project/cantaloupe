package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;

import java.awt.Dimension;

/**
 * Assists in rasterization of vector source images.
 */
class RasterizationHelper {

    private static final int FALLBACK_DPI = 150;

    private int baseDPI = FALLBACK_DPI;

    RasterizationHelper() {
        baseDPI = Configuration.getInstance().
                getInt(Key.PROCESSOR_DPI, FALLBACK_DPI);
    }

    /**
     * @param reductionFactor
     * @return DPI appropriate for the given reduction factor.
     */
    float getDPI(int reductionFactor) {
        float dpi = baseDPI;
        // Decrease the DPI if the reduction factor is positive.
        for (int i = 0; i < reductionFactor; i++) {
            dpi /= 2f;
        }
        // Increase the DPI if the reduction factor is negative.
        for (int i = 0; i > reductionFactor; i--) {
            dpi *= 2f;
        }
        return dpi;
    }

    float getDPI(Scale scale, Dimension fullSize) {
        ReductionFactor reductionFactor = new ReductionFactor();
        Float pct = scale.getResultingScale(fullSize);
        if (pct != null) {
            reductionFactor = ReductionFactor.forScale(pct);
        }
        return getDPI(reductionFactor.factor);
    }

}
