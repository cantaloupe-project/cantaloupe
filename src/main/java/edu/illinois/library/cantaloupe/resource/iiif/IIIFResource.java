package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.resource.PublicResource;

public abstract class IIIFResource extends PublicResource {

    /**
     * When the size expressed in the endpoint URI is {@code max}, and the
     * resulting image dimensions are larger than {@link Key#MAX_PIXELS}, the
     * image must be downscaled to fit that area.
     */
    protected void constrainSizeToMaxPixels(Dimension requestedSize,
                                            OperationList opList) {
        final var config    = Configuration.getInstance();
        final int maxPixels = config.getInt(Key.MAX_PIXELS, 0);
        if (maxPixels > 0 && requestedSize.intArea() > maxPixels) {
            Scale scaleOp = (Scale) opList.getFirst(Scale.class);
            // This should be null because the client requested max size...
            if (scaleOp != null) {
                opList.remove(scaleOp);
            }
            Dimension scaledSize =
                    Dimension.ofScaledArea(requestedSize, maxPixels);
            // The scale dimensions must be floored because rounding up could
            // cause max_pixels to be exceeded.
            scaleOp = new ScaleByPixels(
                    (int) Math.floor(scaledSize.width()),
                    (int) Math.floor(scaledSize.height()),
                    ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            if (opList.getFirst(Crop.class) != null) {
                opList.addAfter(scaleOp, Crop.class);
            } else {
                opList.add(0, scaleOp);
            }
        }
    }

}
