package org.unicode.cldr.util;

import java.math.MathContext;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Rational.RationalParser;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;

public class UnitConverter implements Freezable<UnitConverter> {

    static final Splitter BAR_SPLITTER = Splitter.on('-');

    final RationalParser rationalParser;

    Map<String, Map<String,UnitInfo>> sourceToTargetToInfo = new TreeMap<>();
    Map<String, String> toBaseUnit = new TreeMap<>();
    Set<String> baseUnits;
    Multimap<String, Continuation> continuations = TreeMultimap.create();

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
            .addAll(BASE_UNITS)
            .addAll(toBaseUnit.values())
            .build();
        continuations = ImmutableMultimap.copyOf(continuations);
        return this;
    }

    @Override
    public UnitConverter cloneAsThawed() {
        throw new UnsupportedOperationException();
    }


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
            return factor 
                + (reciprocal ? " / x" : " * x") 
                + (offset.equals(Rational.ZERO) ? "" : 
                    (offset.compareTo(Rational.ZERO) < 0 ? " - " : " - ")
                    + offset.abs());
        }

        public String toDecimal() {
            return factor.toBigDecimal(MathContext.DECIMAL64) 
                + (reciprocal ? " / x" : " * x") 
                + (offset.equals(Rational.ZERO) ? "" : 
                    (offset.compareTo(Rational.ZERO) < 0 ? " - " : " - ")
                    + offset.toBigDecimal(MathContext.DECIMAL64).abs());
        }
    }

    static class Continuation implements Comparable<Continuation> {
        public final List<String> remainder;
        public final String result;

        public static void addIfNeeded(String source, Multimap<String, Continuation> data) {
            List<String> sourceParts = BAR_SPLITTER.splitToList(source);
            if (sourceParts.size() > 1) {
                Continuation continuation = new Continuation(ImmutableList.copyOf(sourceParts.subList(1, sourceParts.size())), source);
                data.put(sourceParts.get(0), continuation);
            }
        }
        private Continuation(List<String> remainder, String source) {
            this.remainder = remainder;
            this.result = source;
        }
        /**
         * The ordering is designed to have longest continuation first so that matching works.
         * Otherwise the ordering doesn't matter, so we just use the result.
         */
        @Override
        public int compareTo(Continuation other) {
            int diff = other.remainder.size() - remainder.size();
            if (diff < 0) {
                return diff;
            }
            return result.compareTo(other.result);
        }

        public boolean match(List<String> parts, final int startIndex) {
            if (remainder.size() > parts.size() - startIndex) {
                return false;
            }
            int i = startIndex;
            for (String unitPart : remainder) {
                if (!unitPart.equals(parts.get(i++))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return remainder + " 🢣 " + result;
        }

        public static Iterable<String> split(String derivedUnit, Multimap<String, Continuation> continuations) {
            return new UnitIterator(derivedUnit, continuations);
        }

        public static class UnitIterator implements Iterable<String>, Iterator<String> {
            final List<String> parts;
            final Multimap<String, Continuation> continuations;
            int nextIndex = 0;

            public UnitIterator(String derivedUnit, Multimap<String, Continuation> continuations) {
                parts = BAR_SPLITTER.splitToList(derivedUnit);
                this.continuations = continuations;
            }

            @Override
            public boolean hasNext() {
                return nextIndex < parts.size();
            }

            @Override
            public String next() {
                String result = parts.get(nextIndex++);
                Collection<Continuation> continuationOptions = continuations.get(result);
                for (Continuation option : continuationOptions) {
                    if (option.match(parts, nextIndex)) {
                        nextIndex += option.remainder.size();
                        return option.result;
                    }
                }
                return result;
            }

            @Override
            public Iterator<String> iterator() {
                return this;
            }

        }
    }

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
        Continuation.addIfNeeded(source, continuations);
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

    static final ImmutableMap<String, String> FIX_DENORMALIZED = ImmutableMap.of(
        "meter-per-second-squared", "meter-per-square-second",
        "liter-per-100kilometers", "liter-per-100-kilometer",
        "pound-foot", "pound-force-foot",
        "pound-per-square-inch", "pound-force-per-square-inch");

    /**
     * Takes a derived unit id, and produces the equivalent derived base unit id and UnitInfo to convert to it
     * @author markdavis
     *
     */
    public UnitInfo parseUnitId (String derivedUnit, Output<String> metricUnit) {
        metricUnit.value = null;

        if (derivedUnit.equals("liter-per-100kilometers")) {
            int debug = 0;
        }
        UnitId outputUnit = new UnitId();
        Rational numerator = Rational.ONE;
        Rational denominator = Rational.ONE;
        boolean inNumerator = true;
        int power = 1;

        Output<Rational> deprefix = new Output<>();

        String fixed = FIX_DENORMALIZED.get(derivedUnit);
        if (fixed != null) {
            derivedUnit = fixed;
        }

        for (String unit : Continuation.split(derivedUnit, continuations)) {

            if (unit.equals("square")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 2;
            } else if (unit.equals("cubic")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 3;
            } else if (unit.startsWith("pow")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = Integer.parseInt(unit.substring(3));
            } else if (unit.equals("per")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of per");
                }
                inNumerator = false; // ignore multiples
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
                // create cleaned up target unitid
                outputUnit.add(continuations, unit, inNumerator, power);
                power = 1;
            }
        }
        metricUnit.value = outputUnit.toString();
        return new UnitInfo(numerator.divide(denominator), Rational.ZERO, false); // fix parameters 2,3 later
    }

    /** Warning: ordering is important; determines the normalized output */
    public static final Set<String> BASE_UNITS = ImmutableSet.of(
        "candela",
        "kilogram", 
        "meter", 
        "second",
        "ampere", 
        "kelvin",
        "mole", 
        // non-SI
        "year", 
        "bit", 
        "item", 
        "pixel", 
        "em", 
        "revolution",
        "portion"
        );
    
    public static final MapComparator<String> UNIT_COMPARATOR = new MapComparator<>(BASE_UNITS)
        .setErrorOnMissing(true)
        .freeze();

    public static final Set<String> BASE_UNIT_PARTS = ImmutableSet.<String>builder()
        .add("per").add("square").add("cubic").addAll(BASE_UNITS)
        .build();
    
    /** 
     * Only handles the canonical units; no kilo-, only normalized, etc.
     * @author markdavis
     *
     */
    public static class UnitId implements Freezable<UnitId> {
        private Map<String, Integer> numUnitsToPowers = new TreeMap<>(UNIT_COMPARATOR);
        private Map<String, Integer> denUnitsToPowers = new TreeMap<>(UNIT_COMPARATOR);
        private boolean frozen = false;

        private UnitId() {} // 
        
        private UnitId add(Multimap<String, Continuation> continuations, String compoundUnit, boolean groupInNumerator, int groupPower) {
            if (frozen) {
                throw new UnsupportedOperationException("Object is frozen.");
            }
            boolean inNumerator = true;
            int power = 1;
            // maybe refactor common parts with above code.
            for (String unitPart : Continuation.split(compoundUnit, continuations)) {
                switch (unitPart) {
                case "square": power = 2; break;
                case "cubic": power = 3; break;
                case "per": inNumerator = false; break; // sticky, ignore multiples
                default: 
                    if (unitPart.startsWith("pow")) {
                        power = Integer.parseInt(unitPart.substring(3));
                    } else {
                        Map<String, Integer> target = inNumerator == groupInNumerator ? numUnitsToPowers : denUnitsToPowers;
                        Integer oldPower = target.get(unitPart);
                        // we multiply powers, so that weight-square-volume => weight-pow4-length
                        int newPower = groupPower * power + (oldPower == null ? 0 : oldPower);
                        target.put(unitPart, newPower);
                        power = 1;
                    }
                }
            }
            return this;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            boolean firstDenominator = true;
            for (int i = 1; i >= 0; --i) { // two passes, numerator then den.
                boolean positivePass = i > 0;
                Map<String, Integer> target = positivePass ? numUnitsToPowers : denUnitsToPowers;
                for (Entry<String, Integer> entry : target.entrySet()) {
                    String unit = entry.getKey();
                    int power = entry.getValue();
                    // NOTE: zero (eg one-per-one) gets counted twice
                    if (builder.length() != 0) {
                        builder.append('-');
                    }
                    if (!positivePass) {
                        if (firstDenominator) {
                            firstDenominator = false;
                            builder.append("per-");
                        }
                    }
                    switch (power) {
                    case 1: 
                        break;
                    case 2: 
                        builder.append("square-"); break;
                    case 3: 
                        builder.append("cubic-"); break;
                    default: 
                        if (power > 3) {
                            builder.append("pow" + power + "-");
                        } else {
                            throw new IllegalArgumentException("Unhandled power: " + power);
                        }
                        break;
                    }
                    builder.append(unit);

                }
            }
            return builder.toString();
        }
        @Override
        public boolean equals(Object obj) {
            UnitId other = (UnitId) obj;
            return numUnitsToPowers.equals(other.numUnitsToPowers) 
                && denUnitsToPowers.equals(other.denUnitsToPowers);
        }
        @Override
        public int hashCode() {
            return Objects.hash(numUnitsToPowers, denUnitsToPowers);
        }
        @Override
        public boolean isFrozen() {
            return frozen;
        }
        @Override
        public UnitId freeze() {
            frozen = true;
            numUnitsToPowers = ImmutableMap.copyOf(numUnitsToPowers);
            denUnitsToPowers = ImmutableMap.copyOf(denUnitsToPowers);
            return this;
        }
        @Override
        public UnitId cloneAsThawed() {
            throw new UnsupportedOperationException();
        }

        public UnitId resolve() {
            UnitId result = new UnitId();
            result.numUnitsToPowers.putAll(numUnitsToPowers);
            result.denUnitsToPowers.putAll(denUnitsToPowers);
            for (Entry<String, Integer> entry : numUnitsToPowers.entrySet()) {
                final String key = entry.getKey();
                Integer denPower = denUnitsToPowers.get(key);
                if (denPower == null) {
                    continue;
                }
                int power = entry.getValue() - denPower;
                if (power > 0) {
                    result.numUnitsToPowers.put(key, power);
                    result.denUnitsToPowers.remove(key);
                } else if (power < 0) {
                    result.numUnitsToPowers.remove(key);
                    result.denUnitsToPowers.put(key, -power);
                } else { // 0, so
                    result.numUnitsToPowers.remove(key);
                    result.denUnitsToPowers.remove(key);
                }
            }
            return result.freeze();
        }
    }

    public final UnitId createUnitId(String unit) {
        return new UnitId().add(continuations, unit, true, 1).freeze();
    }

    public boolean isBaseUnit(String unit) {
        return baseUnits.contains(unit);
    }

    public Set<String> baseUnits() {
        return baseUnits;
    }

    // TODO change to TRIE if the performance isn't good enough, or restructure with regex
    static final ImmutableMap<String, Rational> PREFIXES = ImmutableMap.<String, Rational>builder()
        .put("yocto", Rational.pow10(-24))
        .put("zepto", Rational.pow10(-21))
        .put("atto", Rational.pow10(-18))
        .put("femto", Rational.pow10(-15))
        .put("pico", Rational.pow10(-12))
        .put("nano", Rational.pow10(-9))
        .put("micro", Rational.pow10(-6))
        .put("milli", Rational.pow10(-3))
        .put("centi", Rational.pow10(-2))
        .put("deci", Rational.pow10(-1))
        .put("deka", Rational.pow10(1))
        .put("hecto", Rational.pow10(2))
        .put("kilo", Rational.pow10(3))
        .put("mega", Rational.pow10(6))
        .put("giga", Rational.pow10(9))
        .put("tera", Rational.pow10(12))
        .put("peta", Rational.pow10(15))
        .put("exa", Rational.pow10(18))
        .put("zetta", Rational.pow10(21))
        .put("yotta", Rational.pow10(24))        
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
