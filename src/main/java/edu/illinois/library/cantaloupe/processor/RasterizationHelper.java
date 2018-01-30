package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.Scale;

import java.awt.Dimension;

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
     * @param reductionFactor May be positive, zero, or negative.
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
        Float pct = scale.getResultingScale(fullSize);
        if (pct != null) {
            return baseDPI * scale.getResultingScale(fullSize);
        }
        return baseDPI;
    }

    /**
     * @param dpi DPI at 1x scale.
     */
    void setBaseDPI(int dpi) {
        this.baseDPI = dpi;
    }

}
