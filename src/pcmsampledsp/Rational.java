/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

/**
 * Rational number.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Rational extends Number implements Comparable<Rational> {

    private int numerator;
    private int denominator;

    /**
     * Constructs an immutable rational number.
     *
     * @param numerator numerator
     * @param denominator denominator
     */
    public Rational(final int numerator, final int denominator) {
        if (denominator == 0) throw new IllegalArgumentException("Denominator must not be zero");
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Converts the given float into a rational number using an approximation.
     *
     * @param f decimal
     * @return rational number
     */
    public static Rational valueOf(final float f) {
        // accuracy of 5 decimal places
        return valueOf(f, 0.000005f);
    }

    /**
     * Converts the given float into a rational number using an approximation.
     *
     * @param f decimal
     * @param accuracyFactor accuracy, e.g. <code>0.000005f</code> for five decimal places
     * @return rational number
     */
    public static Rational valueOf(final float f, final float accuracyFactor) {
        final float unsignedFloat = Math.abs(f);
        final int sign = (int)Math.signum(f);

        // cover simple and extreme cases
        if (unsignedFloat==(int)unsignedFloat) {
            return new Rational((int)(unsignedFloat*sign), 1);
        } else if (unsignedFloat < 1.0E-10) {
            return new Rational(sign, 999999999);
        } else if (unsignedFloat>1.0E10) {
            return new Rational(999999999*sign, 1);
        }

        int numerator;
        int denominator = 1;
        float z = unsignedFloat;
        int previousDenominator = 0;
        int scratchValue;

        do {
            z = 1/(z-(int)z);
            scratchValue = denominator;
            denominator = denominator*((int)z)+previousDenominator;
            previousDenominator = scratchValue;

            numerator = Math.round(unsignedFloat*denominator);
        } while (Math.abs((unsignedFloat-(numerator/(float)denominator))) >= accuracyFactor && z != (int)z);

        numerator = sign*numerator;
        return new Rational(numerator, denominator);
    }

    /**
     * Denominator.
     *
     * @return denominator
     */
    public int getDenominator() {
        return denominator;
    }

    /**
     * Numerator
     *
     * @return numerator
     */
    public int getNumerator() {
        return numerator;
    }

    @Override
    public int intValue() {
        return (int)floatValue();
    }

    @Override
    public long longValue() {
        return intValue();
    }

    @Override
    public float floatValue() {
        return numerator / (float)denominator;
    }

    @Override
    public double doubleValue() {
        return numerator / (double)denominator;
    }

    /**
     * Add another rational to this, returning a new object.
     *
     * @param that other rational
     * @return sum
     */
    public Rational add(final Rational that) {
        return new Rational(this.numerator * that.denominator + that.numerator * this.denominator, this.denominator * that.denominator).reduce();
    }

    /**
     * Compute the greatest common divisor (gcd).
     *
     * @return greatest common divisor
     */
    private int greatestCommonDivisor() {
        int a = numerator;
        int b = denominator;
        while (b != 0) {
            int h = a % b;
            a = b;
            b = h;
        }
        return a;
    }

    /**
     * Reduces this, returning a new object, if it is different.
     *
     * @return reduced rational
     */
    public Rational reduce() {
        final int gcd = greatestCommonDivisor();
        if (gcd == numerator) return this;
        else return new Rational(numerator / gcd, denominator / gcd);
    }


    public int compareTo(final Rational that) {
        return Float.compare(this.floatValue(), that.floatValue());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Rational rational = (Rational) o;
        if (denominator != rational.denominator) return false;
        if (numerator != rational.numerator) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = numerator;
        result = 31 * result + denominator;
        return result;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}
