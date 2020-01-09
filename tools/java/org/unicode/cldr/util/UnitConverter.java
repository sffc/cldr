package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.util.Freezable;

public class UnitConverter implements Freezable<UnitConverter> {
    
    static final class Rational {
        public final long numerator;
        public final long denominator;
        public static final Rational ZERO = new Rational(0,1);
        public static final Rational ONE = new Rational(1,1);

        public Rational(String input) {
            input = input.trim();
            int dotPos = input.indexOf('.');
            if (dotPos >= 0) {
                numerator = Long.parseLong(input.replace(".", "")); // optimize later
                denominator = (long) Math.pow(10, input.length() - dotPos - 1);
            } else {
                int slashPos = input.indexOf('/');
                if (slashPos >= 0) {
                    numerator = Long.parseLong(input.substring(0,slashPos));
                    denominator = Long.parseLong(input.substring(slashPos+1));
                } else {
                    numerator = Long.parseLong(input);
                    denominator = 1;
                }
            }
        }

        public Rational(long numerator, long denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

    static final class UnitInfo {
        public final Rational factor;
        public final Rational offset;
        public final boolean reciprocal;
        
        public UnitInfo(Rational factor, Rational offset, boolean reciprocal) {
            this.factor = factor;
            this.offset = offset;
            this.reciprocal = reciprocal;
        }
        
        /** For now, just convert with doubles */
        public double convert(double source) {
            if (reciprocal) {
                source = 1/source;
            }
            return source * factor.numerator / factor.denominator 
                + offset.numerator / (double) offset.denominator;
        }
    }

    final Map<String, Map<String,UnitInfo>> sourceToTargetToInfo = new HashMap<>();

    public void addRaw(String source, String target, String factor, String offset, String reciprocal) {
        UnitInfo info = new UnitInfo(
            factor == null ? Rational.ONE : new Rational(factor), 
                offset == null ? Rational.ZERO : new Rational(offset), 
                    reciprocal == null ? false : reciprocal.equalsIgnoreCase("true") ? true : false);
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(source);
        if (targetToInfo == null) {
            sourceToTargetToInfo.put(source, targetToInfo = new HashMap<>());
        }
        if (targetToInfo.containsKey(target)) {
            throw new IllegalArgumentException("Duplicate source/target: " + source + ", " + target);
        }
        targetToInfo.put(target,info);
    }

    public double convert(double source, String sourceUnit, String targetUnit) {
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(sourceUnit);
        if (targetToInfo == null) {
            return Double.NaN;
        }
        UnitInfo info = targetToInfo.get(targetUnit); 
        if (info == null) {
            return Double.NaN;
        }
        return info.convert(source);
    }

    private boolean frozen = false;
    
    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public UnitConverter freeze() {
        frozen = true;
        CldrUtility.protectCollection(sourceToTargetToInfo);
        return this;
    }

    @Override
    public UnitConverter cloneAsThawed() {
        throw new UnsupportedOperationException();
    }
}
