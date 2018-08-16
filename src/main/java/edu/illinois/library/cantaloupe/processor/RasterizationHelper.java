package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;

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

    double getDPI(final Scale scale,
                  final Dimension fullSize,
                  final ScaleConstraint scaleConstraint) {
        Double pct = scale.getResultingScale(fullSize, scaleConstraint);
        if (pct != null) {
            return baseDPI * pct;
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
