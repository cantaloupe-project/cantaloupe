package edu.illinois.library.cantaloupe.util;

/**
 * Rational number class.
 */
public class Rational {

    private long numerator, denominator;

    public Rational(short numerator, short denominator) {
        this((long) numerator, (long) denominator);
    }

    public Rational(int numerator, int denominator) {
        this((long) numerator, (long) denominator);
    }

    public Rational(long numerator, long denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("Denominator cannot be 0");
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public double doubleValue() {
        return getNumerator() / (double) getDenominator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rational) {
            Rational other = (Rational) obj;
            return other.getNumerator() == getNumerator() &&
                    other.getDenominator() == getDenominator();
        }
        return super.equals(obj);
    }

    public float floatValue() {
        return getNumerator() / (float) getDenominator();
    }

    public long getNumerator() {
        return numerator;
    }

    public long getDenominator() {
        return denominator;
    }

    /**
     * @return New instance reduced to lowest terms, or the same instance if
     *         it is already reduced to lowest terms.
     */
    public Rational getReduced() {
        long n = numerator, d = denominator;

        while (d != 0) {
            long t = d;
            d = n % d;
            n = t;
        }

        long newNumerator = numerator / n, newDenominator = denominator / n;
        if (newNumerator != numerator) {
            return new Rational(newNumerator, newDenominator);
        }
        return this;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return numerator + ":" + denominator;
    }

}
