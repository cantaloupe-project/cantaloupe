package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.util.Rational;

import java.util.Arrays;
import java.util.Map;

/**
 * <p>Represents a scale constraint placed on an image. This is intended for
 * providing dynamic limited-resolution versions of images depending on
 * authorization status.</p>
 *
 * <p>A scale constraint can be thought of as a &quot;virtually&quot;
 * downscaled variant of a source image before any operations have been
 * performed on it. In contrast to a {@link
 * edu.illinois.library.cantaloupe.operation.Scale}, the dimensions of a
 * scale-constrained image appear (to the client) to be the dimensions
 * resulting from the constraint.</p>
 */
public final class ScaleConstraint {

    private final Rational rational;

    /**
     * @param numerator   Scale numerator.
     * @param denominator Scale denominator.
     * @throws IllegalArgumentException if the numerator or denominator is not
     *         positive or if the numerator is greater than the denominator.
     */
    public ScaleConstraint(long numerator, long denominator) {
        if (numerator > denominator) {
            throw new IllegalArgumentException(
                    "Numerator must be less than or equal to denominator");
        } else if (numerator < 1) {
            throw new IllegalArgumentException(
                    "Numerator and denominator must both be positive");
        }
        this.rational = new Rational(numerator, denominator);
    }

    /**
     * @param obj Object to compare.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ScaleConstraint) {
            ScaleConstraint other = (ScaleConstraint) obj;
            return other.getRational().equals(getRational());
        }
        return super.equals(obj);
    }

    /**
     * @param fullSize Full image dimensions.
     * @return         Virtual full size after applying the constraint
     *                 described by the instance.
     */
    public Dimension getConstrainedSize(Dimension fullSize) {
        final double factor = rational.doubleValue();
        return new Dimension(
                fullSize.width() * factor,
                fullSize.height() * factor);
    }

    public Rational getRational() {
        return rational;
    }

    /**
     * @return Instance reduced to lowest terms.
     */
    public ScaleConstraint getReduced() {
        final Rational reduced = getRational().getReduced();
        return new ScaleConstraint(reduced.getNumerator(),
                reduced.getDenominator());
    }

    public Dimension getResultingSize(Dimension fullSize) {
        Dimension size = new Dimension(fullSize);
        size.scale(getRational().doubleValue());
        return size;
    }

    /**
     * @return Whether the instance's {@link Rational#getNumerator()} and
     *         {@link Rational#getDenominator()} are unequal.
     */
    public boolean hasEffect() {
        return (rational.getNumerator() != rational.getDenominator());
    }

    @Override
    public int hashCode() {
        double[] codes = { rational.getNumerator(), rational.getDenominator() };
        return Arrays.hashCode(codes);
    }

    /**
     * @return Map with {@code numerator} and {@code denominator} keys.
     */
    public Map<String,Long> toMap() {
        return Map.of(
                "numerator", rational.getNumerator(),
                "denominator", rational.getDenominator());
    }

    @Override
    public String toString() {
        return rational.getNumerator() + ":" + rational.getDenominator();
    }

}
