package org.unicode.cldr.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Rational.RationalParser;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;

public class UnitConverter implements Freezable<UnitConverter> {

    public static final class UnitInfo {
        public final Rational factor;
        public final Rational offset;
        public final boolean reciprocal;

        static final UnitInfo IDENTITY = new UnitInfo(Rational.ONE, Rational.ZERO, false);

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

        @Override
        public String toString() {
            return factor + (reciprocal ? " / x" : " * x") + (offset == Rational.ZERO ? "" : offset);
        }
    }

    final RationalParser rationalParser;

    Map<String, Map<String,UnitInfo>> sourceToTargetToInfo = new TreeMap<>();
    Map<String, String> toBaseUnit = new TreeMap<>();
    Set<String> baseUnits;

    public UnitConverter(RationalParser rationalParser) {
        this.rationalParser = rationalParser;
    }

    public void addRaw(String source, String target, String factor, String offset, String reciprocal) {
        UnitInfo info = new UnitInfo(
            factor == null ? Rational.ONE : rationalParser.parse(factor), 
                offset == null ? Rational.ZERO : rationalParser.parse(offset), 
                    reciprocal == null ? false : reciprocal.equalsIgnoreCase("true") ? true : false);

        addToSourceToTarget(source, target, info);
        addToSourceToTarget(target, source, info.invert());
        toBaseUnit.put(source, target);
    }

    private void addToSourceToTarget(String source, String target, UnitInfo info) {
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(source);
        if (targetToInfo == null) {
            sourceToTargetToInfo.put(source, targetToInfo = new TreeMap<>());
        }
        if (targetToInfo.containsKey(target)) {
            throw new IllegalArgumentException("Duplicate source/target: " + source + ", " + target);
        }
        targetToInfo.put(target, info);
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

    public Map<String, String> simpleToBaseUnits() {
        return toBaseUnit;
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

    // TODO fix to guarantee single mapping

    public UnitInfo getUnitInfo(String sourceUnit, Output<String> baseUnit) {
        if (isBaseUnit(sourceUnit)) {
            return null;
        }
        Map<String, UnitInfo> targetToInfo = sourceToTargetToInfo.get(sourceUnit);
        if (targetToInfo == null) {
            return null;
        }
        Entry<String, UnitInfo> data = targetToInfo.entrySet().iterator().next();
        baseUnit.value = data.getKey();
        return data.getValue();
    }

    private boolean frozen = false;

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public UnitConverter freeze() {
        frozen = true;
        rationalParser.freeze();
        sourceToTargetToInfo = ImmutableMap.copyOf(sourceToTargetToInfo);
        toBaseUnit = ImmutableMap.copyOf(toBaseUnit);
        baseUnits = ImmutableSet.<String>builder()
            .add("second", 
                "meter", 
                "kilogram", 
                "ampere", 
                "celsius",
                "mole", 
                "candela")
            .addAll(toBaseUnit.values())
            .build();
        return this;
    }

    @Override
    public UnitConverter cloneAsThawed() {
        throw new UnsupportedOperationException();
    }

    static final Splitter BAR_SPLITTER = Splitter.on('-');
    static final ImmutableMap<String, String> FIX_DENORMALIZED = ImmutableMap.of(
        "-second-squared", "-square-second",
        "100kilometers", "100-kilometer");

    /**
     * Takes a derived unit id, and produces the equivalent derived base unit id and UnitInfo to convert to it
     * @author markdavis
     *
     */
    public UnitInfo parseUnitId (String derivedUnit, Output<String> metricUnit) {
        for (Entry<String, String> entry : FIX_DENORMALIZED.entrySet()) {
            String old = entry.getKey();
            String fixed = entry.getValue();
            int index = derivedUnit.indexOf(old);
            if (index >= 0) {
                derivedUnit = derivedUnit.substring(0,index) + fixed + derivedUnit.substring(index + old.length());
            }
        }

        List<String> parts = BAR_SPLITTER.splitToList(derivedUnit);
        StringBuilder outputUnit = new StringBuilder();
        metricUnit.value = null;

        Rational numerator = Rational.ONE;
        Rational denominator = Rational.ONE;
        boolean inNumerator = true;
        int power = 1;

        Output<Rational> deprefix = new Output<>();


        for (int i = 0; i < parts.size(); ++i) {
            // TODO add compound units
            String unit = parts.get(i);
            if (unit.equals("square")) { // TODO pow4
                power = 2;
                if (outputUnit.length() != 0) {
                    outputUnit.append('-');
                }
                outputUnit.append(unit);
                continue;
            } else if (unit.equals("cubic")) {
                power = 3;
                if (outputUnit.length() != 0) {
                    outputUnit.append('-');
                }
                outputUnit.append(unit);
                continue;
            }
            if (unit.equals("per")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                if (!inNumerator) {
                    continue; // ignore multiples
                }
                inNumerator = false;
            } else if ('9' >= unit.charAt(0)) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                Rational factor = Rational.of(Integer.parseInt(unit));
                if (inNumerator) {
                    numerator = numerator.multiply(factor);
                } else {
                    denominator = denominator.multiply(factor);
                }
                continue;
            } else {
                // kilo etc.
                Rational value = Rational.ONE;
                String deprefixed = stripPrefix(unit, deprefix);
                if (deprefixed != null) {
                    unit = deprefixed;
                    value = deprefix.value;
                }
                if (!isBaseUnit(unit)) {
                    String baseUnit = getBaseUnit(unit);
                    if (baseUnit == null) {
                        return null; // can't convert
                    }
                    value = convert(value, unit, baseUnit);
                    unit = baseUnit;
                }
                for (int p = 1; p <= power; ++p) {
                    if (inNumerator) {
                        numerator = numerator.multiply(value);
                    } else {
                        denominator = denominator.multiply(value);
                    }
                }
            }
            if (outputUnit.length() != 0) {
                outputUnit.append('-');
            }
            outputUnit.append(unit);
        }
        metricUnit.value = outputUnit.toString();
        return new UnitInfo(numerator.divide(denominator), Rational.ZERO, false); // fix parameters 2,3 later
    }

    public boolean isBaseUnit(String unit) {
        return baseUnits.contains(unit);
    }

    public Set<String> baseUnits() {
        return baseUnits;
    }

    // TODO change to TRIE if the performance isn't good enough, or restructure with regex
    static final ImmutableMap<String, Rational> PREFIXES = ImmutableMap.<String, Rational>builder()
        .put("yocto", Rational.of(1E-24))
        .put("zepto", Rational.of(1E-21))
        .put("atto", Rational.of(1E-18))
        .put("femto", Rational.of(1E-15))
        .put("pico", Rational.of(1E-12))
        .put("nano", Rational.of(0.000000001))
        .put("micro", Rational.of(0.000001))
        .put("milli", Rational.of(0.001))
        .put("centi", Rational.of(0.01))
        .put("deci", Rational.of(0.1))
        .put("deka", Rational.of(10))
        .put("hecto", Rational.of(100))
        .put("kilo", Rational.of(1000))
        .put("mega", Rational.of(1000000))
        .put("giga", Rational.of(1000000000))
        .put("tera", Rational.of(1000000000000l))
        .put("peta", Rational.of(1000000000000000l))
        .put("exa", Rational.of(1000000000000000000l))
        .put("zetta", Rational.of(1E+21))
        .put("yotta", Rational.of(1E+24))        
        .build();

    private String stripPrefix(String unit, Output<Rational> deprefix) {
        deprefix.value = null;

        for (Entry<String, Rational> entry : PREFIXES.entrySet()) {
            String prefix = entry.getKey();
            if (unit.startsWith(prefix)) {
                deprefix.value = entry.getValue();
                return unit.substring(prefix.length());
            }
        }
        return null;
    }

    public String getBaseUnit(String item) {
        return toBaseUnit.get(item);
    }

}
