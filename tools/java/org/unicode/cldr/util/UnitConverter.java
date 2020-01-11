package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.util.Freezable;

public class UnitConverter implements Freezable<UnitConverter> {

    public static final class UnitInfo {
        public final Rational factor;
        public final Rational offset;
        public final boolean reciprocal;

        public UnitInfo(Rational factor, Rational offset, boolean reciprocal) {
            this.factor = factor;
            this.offset = offset;
            this.reciprocal = reciprocal;
        }

        /** For now, just convert with doubles */
        public Rational convert(Rational source) {
            if (reciprocal) {
                source = source.reciprocal();
            }
            return source.multiply(factor).add(offset);
        }
    }

    final Map<String, Map<String,UnitInfo>> sourceToTargetToInfo = new HashMap<>();

    public void addRaw(String source, String target, String factor, String offset, String reciprocal) {
        UnitInfo info = new UnitInfo(
            factor == null ? Rational.ONE : Rational.of(factor), 
                offset == null ? Rational.ZERO : Rational.of(offset), 
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

    public Rational convert(Rational source, String sourceUnit, String targetUnit) {
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(sourceUnit);
        if (targetToInfo == null) {
            return Rational.NaN;
        }
        UnitInfo info = targetToInfo.get(targetUnit); 
        if (info == null) {
            return Rational.NaN;
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
