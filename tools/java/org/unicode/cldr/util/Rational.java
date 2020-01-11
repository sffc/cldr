package org.unicode.cldr.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Very basic class for rational numbers. No attempt to optimize, since it will just
 * be used for testing within CLDR.
 * 
 * @author markdavis
 *
 */
public final class Rational implements Comparable<Rational> {
    public final BigInteger numerator;
    public final BigInteger denominator;
    // Constraints:
    //   always stored in normalized form. 
    //   no common factor > 1 (reduced)
    //   denominator never negative
    //   if numerator is zero, denominator is 1 or 0
    //   if denominator is zero, numerator is 1, -1, or 0

    public static final Rational ZERO = Rational.of(0,1);
    public static final Rational INFINITY = Rational.of(1,0);
    public static final Rational ONE = Rational.of(1,1);
    public static final Rational NEGATIVE_INFINITY = Rational.of(-1,0);
    public static final Rational NEGATIVE_ONE = Rational.of(-1,1);
    public static final Rational NaN = Rational.of(0,0);

    public static Rational of(String input) {
        input = input.trim();
        BigInteger _numerator;
        BigInteger _denominator;
        int dotPos = input.indexOf('.');
        if (dotPos >= 0) {
            _numerator = new BigInteger(input.replace(".", ""));
            _denominator = BigInteger.valueOf((long)Math.pow(10, input.length() - dotPos - 1));
        } else {
            int slashPos = input.indexOf('/');
            if (slashPos >= 0) {
                _numerator = new BigInteger(input.substring(0,slashPos));
                _denominator = new BigInteger(input.substring(slashPos+1));
            } else {
                _numerator = new BigInteger(input);
                _denominator = BigInteger.ONE;
            }
        }
        return new Rational(_numerator, _denominator);
    }

    public static Rational of(long numerator, long denominator) {
        return new Rational (BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static Rational of(BigInteger numerator, BigInteger denominator) {
        return new Rational (numerator, denominator);
    }

    private Rational(BigInteger numerator, BigInteger denominator) {
        if (denominator.compareTo(BigInteger.ZERO) < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        if (gcd.compareTo(BigInteger.ONE) > 0) {
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }
    
    public Rational add(Rational other) {
        BigInteger gcd_den = denominator.gcd(other.denominator);
        return new Rational(
            numerator.multiply(other.denominator).divide(gcd_den)
            .add(other.numerator.multiply(denominator).divide(gcd_den)),
            denominator.multiply(other.denominator).divide(gcd_den)
            );
    }
    
    public Rational multiply(Rational other) {
        BigInteger gcd_num_oden = numerator.gcd(other.denominator);
        boolean isZero = gcd_num_oden.equals(BigInteger.ZERO);
        BigInteger smallNum = isZero ? numerator : numerator.divide(gcd_num_oden);
        BigInteger smallODen = isZero ? other.denominator : other.denominator.divide(gcd_num_oden);

        BigInteger gcd_den_onum = denominator.gcd(other.numerator);
        isZero = gcd_den_onum.equals(BigInteger.ZERO);
        BigInteger smallONum = isZero ? other.numerator : other.numerator.divide(gcd_den_onum);
        BigInteger smallDen = isZero ? denominator : denominator.divide(gcd_den_onum);
        
        return new Rational(smallNum.multiply(smallONum), smallDen.multiply(smallODen));
    }
    
    public Rational divide(Rational other) {
        return multiply(other.reciprocal());
    }
    
    public Rational reciprocal() {
        return new Rational(denominator, numerator);
    }
    
    public Rational negate() {
        return new Rational(numerator.negate(), denominator);
    }
    
    public BigDecimal toBigDecimal() {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator));
    }
    
    public static Rational of(BigDecimal bigDecimal) {
        // scale()
        // If zero or positive, the scale is the number of digits to the right of the decimal point. 
        // If negative, the unscaled value of the number is multiplied by ten to the power of the negation of the scale. 
        // For example, a scale of -3 means the unscaled value is multiplied by 1000.
        final int scale = bigDecimal.scale();
        final BigInteger unscaled = bigDecimal.unscaledValue();
        if (scale == 0) {
            return new Rational(unscaled, BigInteger.ONE);
        } else if (scale >= 0) {
            return new Rational(unscaled, BigDecimal.ONE.movePointRight(scale).toBigInteger());
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public String toString() {
        // could also return as "exact" decimal, if only factors of the denominator are 2 and 5
        return numerator + (denominator.equals(BigInteger.ONE) ? "" : "/" + denominator);
    }

    @Override
    public int compareTo(Rational other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }
    
    public boolean equals(Object that) {
        return equals((Rational)that); // TODO fix later
    }

    public boolean equals(Rational that) {
        return numerator.equals(that.numerator)
            && denominator.equals(that.denominator);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

}