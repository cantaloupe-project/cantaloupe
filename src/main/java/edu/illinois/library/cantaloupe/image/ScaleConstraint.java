package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.util.Rational;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Represents a scale constraint placed on an image. This is intended for
 * providing dynamic limited-resolution versions of images depending on
 * authorization status.</p>
 *
 * <p>A scale constraint can be thought of as a &quot;virtual resize&quot; of
 * a source image before any operations have been performed on it. In contrast
 * to a {@link edu.illinois.library.cantaloupe.operation.Scale}, the dimensions
 * of a scale-constrained image appear (to the client) to be the dimensions
 * resulting from the constraint.</p>
 */
public final class ScaleConstraint {

    /**
     * Default character sequence separating an identifier from a scale
     * constraint in the identifier portion of a URI.
     */
    private static final String DEFAULT_IDENTIFIER_DELIMITER = "-";

    /**
     * Pattern describing a scale constraint in the identifier portion of a
     * URI, not including the {@link #DEFAULT_IDENTIFIER_DELIMITER}.
     */
    private static final String URI_PATTERN = "(\\d+):(\\d+)\\b";

    private Rational rational;

    /**
     * An identifier URI path component may contain a {@link
     * #getIdentifierSuffixPattern()} scale constraint suffix}. If so, this
     * method will parse it and return an instance reflecting it.
     *
     * @param pathComponent Identifier path component, not URI-decoded.
     * @return              New instance, or {@code null} if the given path
     *                      component does not contain a scale constraint
     *                      suffix.
     */
    public static ScaleConstraint fromIdentifierPathComponent(String pathComponent) {
        if (pathComponent != null) {
            final String decodedComponent = Reference.decode(pathComponent);
            final Matcher matcher =
                    getIdentifierSuffixPattern().matcher(decodedComponent);
            if (matcher.find() && matcher.groupCount() == 2) {
                return new ScaleConstraint(
                        Long.parseLong(matcher.group(1)),
                        Long.parseLong(matcher.group(2)));
            }
        }
        return null;
    }

    /**
     * N.B.: This must be kept in sync with {@link #toIdentifierSuffix()}
     *
     * @return Format of a full scale constraint suffix appended to an
     *         identifier in a URI path component.
     */
    public static Pattern getIdentifierSuffixPattern() {
        var config = Configuration.getInstance();
        return Pattern.compile(
                config.getString(Key.SCALE_CONSTRAINT_DELIMITER,
                        DEFAULT_IDENTIFIER_DELIMITER) +
                        URI_PATTERN);
    }

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
     * N.B.: This must be kept in sync with {@link
     * #getIdentifierSuffixPattern()}.
     */
    public String toIdentifierSuffix() {
        var config = Configuration.getInstance();
        return config.getString(Key.SCALE_CONSTRAINT_DELIMITER,
                DEFAULT_IDENTIFIER_DELIMITER) +
                rational.getNumerator() + ":" + rational.getDenominator();
    }

    /**
     * @return Map with {@code numerator} and {@code denominator} keys.
     */
    public Map<String,Long> toMap() {
        final Map<String,Long> map = new HashMap<>();
        map.put("numerator", rational.getNumerator());
        map.put("denominator", rational.getDenominator());
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return rational.getNumerator() + ":" + rational.getDenominator();
    }

}
