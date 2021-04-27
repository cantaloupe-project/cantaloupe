package edu.illinois.library.cantaloupe.image.exif;

import java.util.Arrays;
import java.util.Map;

/**
 * Rational number class. A zero denominator is allowed.
 *
 * @since 5.0.1
 */
class Rational {

    private final long numerator, denominator;

    Rational(int numerator, int denominator) {
        this(numerator, (long) denominator);
    }

    Rational(long numerator, long denominator) {
        this.numerator   = numerator;
        this.denominator = denominator;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rational) {
            Rational other = (Rational) obj;
            return other.numerator == numerator &&
                    other.denominator == denominator;
        }
        return super.equals(obj);
    }

    long getNumerator() {
        return numerator;
    }

    long getDenominator() {
        return denominator;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new long[] { numerator, denominator });
    }

    /**
     * @return Unmodifiable map with {@code numerator} and {@code denominator}
     *         keys.
     */
    Map<String,Long> toMap() {
        return Map.of("numerator", getNumerator(),
                "denominator", getDenominator());
    }

    @Override
    public String toString() {
        return numerator + ":" + denominator;
    }

}
