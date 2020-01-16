package org.unicode.cldr.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Rational.RationalParser;

import com.google.common.collect.ImmutableMap;
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

        public UnitInfo invert() {
            Rational factor2 = factor.reciprocal();
            Rational offset2 = offset.equals(Rational.ZERO) ? Rational.ZERO : offset.divide(factor).negate();
            return new UnitInfo(factor2, offset2, reciprocal);
            // TODO fix reciprocal
        }
    }

    final Map<String, Map<String,UnitInfo>> sourceToTargetToInfo = new HashMap<>();
    
    final static RationalParser bootstrapParser = new Rational.RationalParser(ImmutableMap.of(
        "f2m", Rational.of(new BigDecimal("0.3048"))));
    final static RationalParser rationalParser =  new Rational.RationalParser(ImmutableMap.of(
        "f2m", Rational.of(new BigDecimal("0.3048")),
        "lb2kg", Rational.of(new BigDecimal("0.45359237")),
        "gravity", Rational.of(new BigDecimal("9.80665")),
        "PI", Rational.of(new BigDecimal("3.1415926535897932384626433832795")),
        "cup2m3", bootstrapParser.parse("231*f2m*f2m*f2m/16*12*12*12")
        // cup/gallon = 1/16
        // gallon/inch3 = 231
        // inch3/foot3 = 1/(144*144*144)
        ));


    public void addRaw(String source, String target, String factor, String offset, String reciprocal) {
        UnitInfo info = new UnitInfo(
            factor == null ? Rational.ONE : rationalParser.parse(factor), 
                offset == null ? Rational.ZERO : rationalParser.parse(offset), 
                    reciprocal == null ? false : reciprocal.equalsIgnoreCase("true") ? true : false);
        
        addToSourceToTarget(source, target, info);
        addToSourceToTarget(target, source, info.invert());
    }

    private void addToSourceToTarget(String source, String target, UnitInfo info) {
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(source);
        if (targetToInfo == null) {
            sourceToTargetToInfo.put(source, targetToInfo = new HashMap<>());
        }
        if (targetToInfo.containsKey(target)) {
            throw new IllegalArgumentException("Duplicate source/target: " + source + ", " + target);
        }
        targetToInfo.put(target,info);
    }
    
    public Set<String> canConvertBetween(String unit) {
        Set<String> result = new TreeSet<>();
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(unit);
        if (targetToInfo == null) {
            return Collections.emptySet();
        }
        result.addAll(targetToInfo.keySet());
        for (String pivot : targetToInfo.keySet()) {
            Map<String, UnitInfo> pivotToInfo = sourceToTargetToInfo.get(pivot);
            result.addAll(pivotToInfo.keySet());
        }
        return result;
    }
    
    public Set<String> canConvert() {
        return sourceToTargetToInfo.keySet();
    }

    public Rational convert(Rational source, String sourceUnit, String targetUnit) {
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(sourceUnit);
        if (targetToInfo == null) {
            return Rational.NaN;
        }
        UnitInfo info = targetToInfo.get(targetUnit); 
        if (info != null) {
            return info.convert(source);
        }
        // try pivot
        Map<String, UnitInfo> sourceToInfo = sourceToTargetToInfo.get(targetUnit);
        if (sourceToInfo == null) {
            return Rational.NaN;
        }
        HashSet<String> pivots = new HashSet<>(targetToInfo.keySet());
        pivots.retainAll(sourceToInfo.keySet());
        if (pivots.isEmpty()) {
            return Rational.NaN;
        }
        String pivot = pivots.iterator().next();
        info = targetToInfo.get(pivot);
        Rational temp = info.convert(source);
        
        Map<String, UnitInfo> pivotToInfo = sourceToTargetToInfo.get(pivot);
        UnitInfo info2 = pivotToInfo.get(targetUnit);
        return info2.convert(temp);
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
