package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

public class CropToSquare extends Crop implements Operation {

    @Override
    public Rectangle getRectangle(Dimension reducedSize,
                                  ReductionFactor reductionFactor,
                                  ScaleConstraint scaleConstraint) {
        final double shortestSide = Math.min(
                reducedSize.width(), reducedSize.height());
        final double x = (reducedSize.width() - shortestSide) / 2.0;
        final double y = (reducedSize.height() - shortestSide) / 2.0;
        return new Rectangle(x, y, shortestSide, shortestSide);
    }

    /**
     * May produce false positives. {@link #hasEffect(Dimension,
     * OperationList)} should be used instead where possible.
     *
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return fullSize.intWidth() != fullSize.intHeight();
    }

    @Override
    public String toString() {
        return "square";
    }

}
