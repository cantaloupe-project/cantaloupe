package edu.illinois.library.cantaloupe.resource.iiif;

import java.util.Arrays;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.resource.ScaleRestrictedException;

/**
 * Shared scale validation logic for IIIF.
 * Note that there is an additional scale validator contained within v3.ImageResource
 */
public class ScaleValidator {
    
    /**
     * @param virtualSize   Orientation-aware full source image size.
     * @param scale         May be {@code null}.
     * @param invalidStatus Status code to return when the given scale fails
     *                      validation.
     */
    public static void validateScale(Dimension virtualSize,
                                 Scale scale,
                                 Status invalidStatus,
                                 MetaIdentifier metaId) throws ScaleRestrictedException {
        final ScaleConstraint scaleConstraint = (metaId.getScaleConstraint() != null) ?
                metaId.getScaleConstraint() : new ScaleConstraint(1, 1);
        double scalePct = scaleConstraint.getRational().doubleValue();
        if (scale != null) {
            scalePct = Arrays.stream(
                    scale.getResultingScales(virtualSize, scaleConstraint))
                    .max().orElse(1);
        }
        final Configuration config = Configuration.getInstance();
        final double maxScale      = config.getDouble(Key.MAX_SCALE, 1.0);
        if (maxScale > 0.0001 && scalePct > maxScale) {
            throw new ScaleRestrictedException(invalidStatus, maxScale);
        }
    }
    
}
