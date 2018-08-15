package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.util.StringUtils;

import java.awt.Dimension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a rotation operation.
 */
public class Rotate implements Operation {

    private static final double DELTA = 0.00000001;

    private double degrees;
    private boolean isFrozen;

    /**
     * No-op constructor.
     */
    public Rotate() {}

    /**
     * @param degrees Degrees of rotation between 0 and 360.
     */
    public Rotate(double degrees) {
        this();
        setDegrees(degrees);
    }

    /**
     * @param degrees Degrees to add.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void addDegrees(double degrees) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.degrees = (this.degrees + degrees) % 360;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rotate) {
            Rotate other = (Rotate) obj;
            return (Math.abs(getDegrees() - other.getDegrees()) < DELTA);
        }
        return super.equals(obj);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return Degrees.
     */
    public double getDegrees() {
        return degrees;
    }

    /**
     * @param fullSize
     * @return Resulting dimensions when the scale is applied to the given full
     *         size.
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize,
                                      ScaleConstraint scaleConstraint) {
        final double radians = Math.toRadians(getDegrees());
        final double sin = Math.sin(radians);
        final double cos = Math.cos(radians);

        final int width = (int) Math.round(
                Math.abs(fullSize.width * cos) + Math.abs(fullSize.height * sin));
        final int height = (int) Math.round(
                Math.abs(fullSize.height * cos) + Math.abs(fullSize.width * sin));
        return new Dimension(width, height);
    }

    @Override
    public boolean hasEffect() {
        return (Math.abs(getDegrees()) > 0.0001f);
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    @Override
    public int hashCode() {
        return Double.hashCode(degrees);
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalArgumentException If the given degrees are invalid.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setDegrees(double degrees) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (degrees < 0 || degrees >= 360) {
            throw new IllegalArgumentException("Degrees must be between 0 and 360");
        }
        this.degrees = degrees;
    }

    /**
     * <p>Returns a map in the following format:</p>
     *
     * <pre>{
     *     class: "Rotate",
     *     degrees: double
     * }</pre>
     *
     * @return See above.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("degrees", getDegrees());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent it.
     */
    @Override
    public String toString() {
        return StringUtils.removeTrailingZeroes(getDegrees());
    }

}
