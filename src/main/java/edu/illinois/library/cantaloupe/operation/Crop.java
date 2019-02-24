package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Abstract cropping operation.</p>
 *
 * <p>The basic idea is that {@link #getRectangle} is used to compute a source
 * image region based on arguments supplied to it and to subclasses' mutator
 * methods.</p>
 */
public abstract class Crop implements Operation {

    static final double DELTA = 0.00000001;

    Orientation orientation = Orientation.ROTATE_0;

    private boolean isFrozen;

    void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Crop) {
            return obj.toString().equals(toString());
        }
        return super.equals(obj);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @param fullSize Full-sized image dimensions.
     * @return         Rectangle relative to the given full dimensions.
     */
    public Rectangle getRectangle(Dimension fullSize) {
        return getRectangle(fullSize,
                new ReductionFactor(),
                new ScaleConstraint(1, 1));
    }

    /**
     * @param fullSize        Full-sized image dimensions.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@literal fullSize}.
     * @return                Rectangle relative to the given full dimensions.
     */
    public Rectangle getRectangle(Dimension fullSize,
                                  ScaleConstraint scaleConstraint) {
        return getRectangle(fullSize, new ReductionFactor(), scaleConstraint);
    }

    /**
     * Computes an effective crop rectangle in source image coordinates.
     *
     * @param reducedSize     Size of the input image, reduced by {@literal
     *                        reductionFactor}.
     * @param reductionFactor Factor by which the full-sized image has been
     *                        reduced to become {@literal reducedSize}.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@literal reducedSize}
     *                        or the full image size.
     * @return                Rectangle relative to the given reduced
     *                        dimensions.
     */
    abstract public Rectangle getRectangle(Dimension reducedSize,
                                           ReductionFactor reductionFactor,
                                           ScaleConstraint scaleConstraint);

    @Override
    public Dimension getResultingSize(Dimension fullSize,
                                      ScaleConstraint scaleConstraint) {
        return getRectangle(fullSize, scaleConstraint).size();
    }

    /**
     * This method may produce false positives. {@link #hasEffect(Dimension,
     * OperationList)} should be used instead where possible, unless overrides
     * mention otherwise.
     *
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public abstract boolean hasEffect();

    /**
     * @param fullSize
     * @param opList
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public abstract boolean hasEffect(Dimension fullSize,
                                      OperationList opList);

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Hints that the instance should be adapted to an image that is to be
     * treated as rotated. (As in e.g. the case of an EXIF {@literal
     * Orientation} tag describing the rotation of un-rotated image data.)
     *
     * @param orientation Orientation of the image.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setOrientation(Orientation orientation) {
        checkFrozen();
        this.orientation = orientation;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return         Map with {@literal class}, {@literal x}, {@literal y},
     *                 {@literal width}, and {@literal height} keys and integer
     *                 values corresponding to the instance coordinates.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Rectangle rect = getRectangle(fullSize, scaleConstraint);
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("x", rect.intX());
        map.put("y", rect.intY());
        map.put("width", rect.intWidth());
        map.put("height", rect.intHeight());
        return Collections.unmodifiableMap(map);
    }

    /**
     * Checks the crop intersection and dimensions.
     *
     * {@inheritDoc}
     */
    @Override
    public void validate(Dimension fullSize,
                         ScaleConstraint scaleConstraint)
            throws ValidationException {
        try {
            Dimension resultingSize =
                    getResultingSize(fullSize, scaleConstraint);
            if (resultingSize.isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Crop area is outside the bounds of the source image.");
        }
    }

}
