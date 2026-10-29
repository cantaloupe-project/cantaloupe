package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.operation.ValidationException;

public class SizeConstrainer {
    
    /**
     * When the size expressed in the endpoint URI is {@code max}, and the
     * resulting image dimensions are larger than {@link Key#MAX_PIXELS}, the
     * image must be downscaled to fit that area.
     * 
     * @param requestedSize  Full size of the source image.
     * @param opList OperationsList. This gets mutated if the requested size is too big
     * @throws ValidationException if a cropping Operation is invalid.
     */
    public static void constrainSizeToMaxPixels(Dimension requestedSize,
                                            OperationList opList,
                                            Configuration config) throws ValidationException {
        final int maxPixels = config.getInt(Key.MAX_PIXELS, 0);
        // This ensures we compare maxPixels against the Resulting size 
        // after operations like cropping/region are applied.
        Operation cropOp = opList.getFirst(Crop.class);
        if (cropOp != null) {
            // Crop arguments could be wrong or out of bounds
            // and we might get an internal exception thrown on validate().
            cropOp.validate(requestedSize, opList.getScaleConstraint());
            requestedSize = cropOp.getResultingSize(requestedSize, opList.getScaleConstraint());
        }
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
