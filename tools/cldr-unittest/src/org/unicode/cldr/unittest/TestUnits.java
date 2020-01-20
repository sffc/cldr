package org.unicode.cldr.unittest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.UnitInfo;
import org.unicode.cldr.util.Units;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.Output;

public class TestUnits extends TestFmwk {
    private static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    CLDRConfig info = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestUnits().run(args);
    }

    public void TestSpaceInNarrowUnits() {
        final CLDRFile english = CLDRConfig.getInstance().getEnglish();
        final Matcher m = Pattern.compile("narrow.*unitPattern").matcher("");
        for (String path : english) {
            if (m.reset(path).find()) {
                String value = english.getStringValue(path);
                if (value.contains("} ")) {
                    errln(path + " fails, «" + value + "» contains } + space");
                }
            }
        }
    }

    static final String[][] COMPOUND_TESTS = {
        {"area-square-centimeter", "square", "length-centimeter"},
        {"area-square-foot", "square", "length-foot"},
        {"area-square-inch", "square", "length-inch"},
        {"area-square-kilometer", "square", "length-kilometer"},
        {"area-square-meter", "square", "length-meter"},
        {"area-square-mile", "square", "length-mile"},
        {"area-square-yard", "square", "length-yard"},
        {"digital-gigabit", "giga", "digital-bit"},
        {"digital-gigabyte", "giga", "digital-byte"},
        {"digital-kilobit", "kilo", "digital-bit"},
        {"digital-kilobyte", "kilo", "digital-byte"},
        {"digital-megabit", "mega", "digital-bit"},
        {"digital-megabyte", "mega", "digital-byte"},
        {"digital-petabyte", "peta", "digital-byte"},
        {"digital-terabit", "tera", "digital-bit"},
        {"digital-terabyte", "tera", "digital-byte"},
        {"duration-microsecond", "micro", "duration-second"},
        {"duration-millisecond", "milli", "duration-second"},
        {"duration-nanosecond", "nano", "duration-second"},
        {"electric-milliampere", "milli", "electric-ampere"},
        {"energy-kilocalorie", "kilo", "energy-calorie"},
        {"energy-kilojoule", "kilo", "energy-joule"},
        {"frequency-gigahertz", "giga", "frequency-hertz"},
        {"frequency-kilohertz", "kilo", "frequency-hertz"},
        {"frequency-megahertz", "mega", "frequency-hertz"},
        {"graphics-megapixel", "mega", "graphics-pixel"},
        {"length-centimeter", "centi", "length-meter"},
        {"length-decimeter", "deci", "length-meter"},
        {"length-kilometer", "kilo", "length-meter"},
        {"length-micrometer", "micro", "length-meter"},
        {"length-millimeter", "milli", "length-meter"},
        {"length-nanometer", "nano", "length-meter"},
        {"length-picometer", "pico", "length-meter"},
        {"mass-kilogram", "kilo", "mass-gram"},
        {"mass-microgram", "micro", "mass-gram"},
        {"mass-milligram", "milli", "mass-gram"},
        {"power-gigawatt", "giga", "power-watt"},
        {"power-kilowatt", "kilo", "power-watt"},
        {"power-megawatt", "mega", "power-watt"},
        {"power-milliwatt", "milli", "power-watt"},
        {"pressure-hectopascal", "hecto", "pressure-pascal"},
        {"pressure-millibar", "milli", "pressure-bar"},
        {"pressure-kilopascal", "kilo", "pressure-pascal"},
        {"pressure-megapascal", "mega", "pressure-pascal"},
        {"volume-centiliter", "centi", "volume-liter"},
        {"volume-cubic-centimeter", "cubic", "length-centimeter"},
        {"volume-cubic-foot", "cubic", "length-foot"},
        {"volume-cubic-inch", "cubic", "length-inch"},
        {"volume-cubic-kilometer", "cubic", "length-kilometer"},
        {"volume-cubic-meter", "cubic", "length-meter"},
        {"volume-cubic-mile", "cubic", "length-mile"},
        {"volume-cubic-yard", "cubic", "length-yard"},
        {"volume-deciliter", "deci", "volume-liter"},
        {"volume-hectoliter", "hecto", "volume-liter"},
        {"volume-megaliter", "mega", "volume-liter"},
        {"volume-milliliter", "milli", "volume-liter"},
    };

    static final String[][] PREFIX_NAME_TYPE = {
        {"deci", "10p-1"},
        {"centi", "10p-2"},
        {"milli", "10p-3"},
        {"micro", "10p-6"},
        {"nano", "10p-9"},
        {"pico", "10p-12"},
        {"femto", "10p-15"},
        {"atto", "10p-18"},
        {"zepto", "10p-21"},
        {"yocto", "10p-24"},
        {"deka", "10p1"},
        {"hecto", "10p2"},
        {"kilo", "10p3"},
        {"mega", "10p6"},
        {"giga", "10p9"},
        {"tera", "10p12"},
        {"peta", "10p15"},
        {"exa", "10p18"},
        {"zetta", "10p21"},
        {"yotta", "10p24"},
        {"square", "power2"},
        {"cubic", "power3"},
    };

    static final String PATH_UNIT_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"{1}\"]/unitPattern[@count=\"{2}\"]";

    static final String PATH_PREFIX_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/unitPrefixPattern";
    static final String PATH_SUFFIX_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/compoundUnitPattern1";

    static final String PATH_MILLI_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"10p-3\"]/unitPrefixPattern";
    static final String PATH_SQUARE_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1";


    static final String PATH_METER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"{1}\"]";
    static final String PATH_MILLIMETER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"length-millimeter\"]/unitPattern[@count=\"{1}\"]";
    static final String PATH_SQUARE_METER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"area-square-meter\"]/unitPattern[@count=\"{1}\"]";    

    public void TestCompoundUnit3() {
        Factory factory = CLDRConfig.getInstance().getCldrFactory();

        Map<String,String> prefixToType = new LinkedHashMap<>();
        for (String[] prefixRow : PREFIX_NAME_TYPE) {
            prefixToType.put(prefixRow[0], prefixRow[1]);
        }
        prefixToType = ImmutableMap.copyOf(prefixToType);

        Set<String> localesToTest = ImmutableSet.of("en"); // factory.getAvailableLanguages();
        int testCount = 0;
        for (String locale : localesToTest) {
            CLDRFile file = factory.make(locale, true);
            //ExampleGenerator exampleGenerator = getExampleGenerator(locale);
            PluralInfo pluralInfo = SDI.getPlurals(PluralType.cardinal, locale);
            final boolean isEnglish = locale.contentEquals("en");
            int errMsg = isEnglish ? ERR : WARN;

            for (String[] compoundTest : COMPOUND_TESTS) {
                String targetUnit = compoundTest[0];
                String prefix = compoundTest[1];
                String baseUnit = compoundTest[2];
                String prefixType = prefixToType.get(prefix); // will be null for square, cubic
                final boolean isPrefix = prefixType.startsWith("1");

                for (String len : Arrays.asList("long", "short", "narrow")) {
                    String prefixPath = ExampleGenerator.format(isPrefix ? PATH_PREFIX_PATTERN 
                        : PATH_SUFFIX_PATTERN,
                        len, prefixType);
                    String prefixValue = file.getStringValue(prefixPath);
                    boolean lowercaseIfSpaced = len.equals("long");

                    for (Count count : pluralInfo.getCounts()) {
                        final String countString = count.toString();
                        String targetUnitPath = ExampleGenerator.format(PATH_UNIT_PATTERN, len, targetUnit, countString);
                        String targetUnitPattern = file.getStringValue(targetUnitPath);

                        String baseUnitPath = ExampleGenerator.format(PATH_UNIT_PATTERN, len, baseUnit, countString);
                        String baseUnitPattern = file.getStringValue(baseUnitPath);

                        String composedTargetUnitPattern = Units.combinePattern(baseUnitPattern, prefixValue, lowercaseIfSpaced);
                        if (isEnglish && !targetUnitPattern.equals(composedTargetUnitPattern)) {
                            if (allowEnglishException(targetUnitPattern, composedTargetUnitPattern)) {
                                continue;
                            }
                        }
                        if (!assertEquals2(errMsg, testCount++ + ") " 
                            + locale + "/" + len + "/" + count + "/" + prefix + "+" + baseUnit 
                            + ": constructed pattern",
                            targetUnitPattern, 
                            composedTargetUnitPattern)) {
                            Units.combinePattern(baseUnitPattern, prefixValue, lowercaseIfSpaced);
                            int debug = 0;
                        };
                    }
                }
            }
        }
    }

    /**
     * Curated list of known exceptions. Usually because the short form of a unit is shorter when combined with a prefix or suffix
     */
    static final Map<String,String> ALLOW_ENGLISH_EXCEPTION = ImmutableMap.<String,String>builder()
        .put("sq ft", "ft²")
        .put("sq mi", "mi²")
        .put("ft", "′")
        .put("in", "″")
        .put("MP", "Mpx")
        .put("b", "bit")
        .put("mb", "mbar")
        .put("B", "byte")
        .put("s", "sec")
        .build();
    private boolean allowEnglishException(String targetUnitPattern, String composedTargetUnitPattern) {
        for (Entry<String, String> entry : ALLOW_ENGLISH_EXCEPTION.entrySet()) {
            String mod = targetUnitPattern.replace(entry.getKey(), entry.getValue());
            if (mod.contentEquals(composedTargetUnitPattern)) {
                return true;
            }
        }
        return false;
    }

    // TODO Work this into a generating and then maintaining a data table for the units
    /*
        CLDRFile english = factory.make("en", false);
        Set<String> prefixes = new TreeSet<>();
        for (String path : english) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String lastElement = parts.getElement(-1);
            if (lastElement.equals("unitPrefixPattern") || lastElement.equals("compoundUnitPattern1")) {
                if (!parts.getAttributeValue(2, "type").equals("long")) {
                    continue;
                }
                String value = english.getStringValue(path);
                prefixes.add(value.replace("{0}", "").trim());
            }
        }        
        Map<Status, Set<String>> unitValidity = Validity.getInstance().getStatusToCodes(LstrType.unit);
        Multimap<String, String> from = LinkedHashMultimap.create();
        for (String unit : unitValidity.get(Status.regular)) {
            String[] parts = unit.split("[-]");
            String main = parts[1];
            for (String prefix : prefixes) {
                if (main.startsWith(prefix)) {
                    if (main.length() == prefix.length()) { // square,...
                        from.put(unit, main);
                    } else { // milli
                        from.put(unit, main.substring(0,prefix.length()));
                        from.put(unit, main.substring(prefix.length()));
                    }
                    for (int i = 2; i < parts.length; ++i) {
                        from.put(unit, parts[i]);
                    }
                }
            }
        }
        for (Entry<String, Collection<String>> set : from.asMap().entrySet()) {
            System.out.println(set.getKey() + "\t" + CollectionUtilities.join(set.getValue(), "\t"));
        }
     */
    private boolean assertEquals2(int TestERR, String title, String sqmeterPattern, String conSqmeterPattern) {
        if (!Objects.equals(sqmeterPattern, conSqmeterPattern)) {
            msg(title + ", expected «" + sqmeterPattern + "», got «" + conSqmeterPattern + "»", TestERR, true, true);
            return false;
        } else if (isVerbose()) {
            msg(title + ", expected «" + sqmeterPattern + "», got «" + conSqmeterPattern + "»", LOG, true, true);
        }
        return true;
    }

    static final boolean DEBUG = false;

    public void TestConversion() {
        UnitConverter converter = SDI.getUnitConverter();
        Object[][] tests = {
            {"foot", 12, "inch"},
            {"gallon", 4, "quart"},
            {"gallon", 16, "cup"},
        };
        for (Object[] test : tests) {
            String sourceUnit = test[0].toString();
            String targetUnit = test[2].toString();
            int numerator = (Integer) test[1];
            assertEquals(sourceUnit + " to " + targetUnit, Rational.of(numerator, 1), converter.convert(Rational.ONE, sourceUnit, targetUnit));
        }

        // test conversions are disjoint
        Set<String> gotAlready = new HashSet<>();
        List<Set<String>> equivClasses = new ArrayList<>();
        Map<String,String> classToId = new TreeMap<>();
        for (String unit : converter.canConvert()) {
            if (gotAlready.contains(unit)) {
                continue;
            }
            Set<String> set = converter.canConvertBetween(unit);
            final String id = "ID" + equivClasses.size();
            equivClasses.add(set);
            gotAlready.addAll(set);
            for (String s : set) {
                classToId.put(s, id);
            }
        }

        // check not overlapping
        for (int i = 0; i < equivClasses.size(); ++i) {
            Set<String> eclass1 = equivClasses.get(i);
            for (int j = i+1; j < equivClasses.size(); ++j) {
                Set<String> eclass2 = equivClasses.get(j);
                if (!Collections.disjoint(eclass1, eclass2)) {
                    errln("Overlapping equivalence classes: " + eclass1 + " ~ " + eclass2 + "\n\tProbably bad chain requiring 3 steps.");
                }
            }

            // check that all elements of an equivalence class have the same type
            Multimap<String,String> breakdown = TreeMultimap.create();
            for (String item : eclass1) {
                String type = CORE_TO_TYPE.get(item);
                if (type == null) {
                    type = "?";
                }
                breakdown.put(type, item);
            }
            if (DEBUG) System.out.println("type to item: " + breakdown);
            if (breakdown.keySet().size() != 1) {
                errln("mixed categories: " + breakdown);
            }

        }

        // check that all units with the same type have the same equivalence class
        for (Entry<String, Collection<String>> entry : TYPE_TO_CORE.asMap().entrySet()) {
            Multimap<String,String> breakdown = TreeMultimap.create();
            for (String item : entry.getValue()) {
                String id = classToId.get(item);
                if (id == null) {
                    continue;
                }
                breakdown.put(id, item);
            }
            if (DEBUG) System.out.println(entry.getKey() + " id to item: " + breakdown);
            if (breakdown.keySet().size() != 1) {
                errln(entry.getKey() + " mixed categories: " + breakdown);
            }
        }
    }

    public void TestBaseUnits() {
        UnitConverter converter = SDI.getUnitConverter();
        Splitter barSplitter = Splitter.on('-');
        ImmutableSet<String> allowedInBase = ImmutableSet.of(
            "second", "meter","kilogram", "ampere", "celsius", // kelvin
            "mole", "candela", 
            "per", "square", "cubic", 
            "one", "bit", "degree", // instead of radian
            "year", "pixel", "em"
            );
        for (String unit : converter.baseUnits()) {
            for (String piece : barSplitter.split(unit)) {
                assertTrue(unit + ": " + piece + " in " + allowedInBase, allowedInBase.contains(piece));
            }
        }
    }

    public void TestParseUnit() {
        UnitConverter converter = SDI.getUnitConverter();
        Output<String> compoundBaseUnit = new Output<>();
        String[][] tests = {
            {"kilometer-pound-per-hour", "meter-kilogram-per-second", "45359237/360000000"},
            {"kilometer-per-hour", "meter-per-second", "5/18"},
        };
        for (String[] test : tests) {
            String source = test[0];
            String expectedUnit = test[1];
            Rational expectedRational = new Rational.RationalParser().parse(test[2]);
            UnitInfo unitInfo = converter.parseUnitId(source, compoundBaseUnit);
            assertEquals(source, expectedUnit, compoundBaseUnit.value);
            assertEquals(source, expectedRational, unitInfo.factor);
        }

        // check all 
        System.out.println();
        Set<String> badUnits = new LinkedHashSet<>();
        
        // checkUnitConvertability(converter, compoundBaseUnit, badUnits, "pint-metric-per-second");

        for (Entry<String, String> entry : TYPE_TO_CORE.entries()) {
            String type = entry.getKey();
            String unit = entry.getValue();
            checkUnitConvertability(converter, compoundBaseUnit, badUnits, type, unit);
        }
        assertEquals("Unconvertable units", Collections.emptySet(), badUnits);
    }

    private void checkUnitConvertability(UnitConverter converter, Output<String> compoundBaseUnit, 
        Set<String> badUnits, String type, String unit) {
        if (unit.equals("generic")) {
            return;
        }
        if (unit.equals("milliampere")) {
            int debug = 0;
        }
        if (converter.isBaseUnit(unit)) {
            System.out.println(type 
                + "\t" + unit
                + "\t" + unit);
        } else {
            UnitInfo unitInfo = converter.getUnitInfo(unit, compoundBaseUnit);
            if (unitInfo == null) {
                unitInfo = converter.parseUnitId(unit, compoundBaseUnit);
            }
            if (unitInfo == null) {
                badUnits.add(unit);
            } else {
            System.out.println(
                type 
                + "\t" + unit
                + "\t" + compoundBaseUnit
                + "\t" + unitInfo.factor.toBigDecimal(MathContext.DECIMAL32)
                + "\t" + unitInfo.factor.reciprocal().toBigDecimal(MathContext.DECIMAL32)
                + "\t" + unitInfo.toDecimal()
                + "\t" + unitInfo
                );
            }
        }
    }

    public void TestRational() {
        Rational a3_5 = Rational.of(3,5);

        Rational a6_10 = Rational.of(6,10);
        assertEquals("", a3_5, a6_10);

        Rational a5_3 = Rational.of(5,3);
        assertEquals("", a3_5, a5_3.reciprocal());

        assertEquals("", Rational.ONE, a3_5.multiply(a3_5.reciprocal()));
        assertEquals("", Rational.ZERO, a3_5.add(a3_5.negate()));

        assertEquals("", Rational.INFINITY, Rational.ZERO.reciprocal());
        assertEquals("", Rational.NEGATIVE_INFINITY, Rational.INFINITY.negate());
        assertEquals("", Rational.NEGATIVE_ONE, Rational.ONE.negate());

        assertEquals("", Rational.NaN, Rational.ZERO.divide(Rational.ZERO));

        assertEquals("", BigDecimal.valueOf(2), Rational.of(2,1).toBigDecimal());
        assertEquals("", BigDecimal.valueOf(0.5), Rational.of(1,2).toBigDecimal());

        assertEquals("", BigDecimal.valueOf(100), Rational.of(100,1).toBigDecimal());
        assertEquals("", BigDecimal.valueOf(0.01), Rational.of(1,100).toBigDecimal());

        assertEquals("", Rational.of(12370,1), Rational.of(BigDecimal.valueOf(12370)));
        assertEquals("", Rational.of(1237,10), Rational.of(BigDecimal.valueOf(1237.0/10)));
        assertEquals("", Rational.of(1237,10000), Rational.of(BigDecimal.valueOf(1237.0/10000)));
    }

    public void TestRationalParse() {
        Rational.RationalParser parser = SDI.getRationalParser();

        Rational a3_5 = Rational.of(3,5);

        assertEquals("", a3_5, parser.parse("6/10"));

        assertEquals("", a3_5, parser.parse("0.06/0.10"));

        assertEquals("", Rational.of(381, 1250), parser.parse("ft2m"));
        assertEquals("", 6.02214076E+23d, parser.parse("6.02214076E+23").toBigDecimal().doubleValue());
        Rational temp = parser.parse("cup2m3");
        //System.out.println(" " + temp);
        assertEquals("", 2.365882365E-4d, temp.numerator.doubleValue()/temp.denominator.doubleValue());

    }


    static final Map<String,String> CORE_TO_TYPE;
    static final Multimap<String,String> TYPE_TO_CORE;
    static final Set<String> VALID_UNITS;
    static {
        VALID_UNITS = Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular);

        Map<String, String> coreToType = new TreeMap<>();
        TreeMultimap<String, String> typeToCore = TreeMultimap.create();
        for (String s : VALID_UNITS) {
            int dashPos = s.indexOf('-');
            String unitType = s.substring(0,dashPos);
            String coreUnit = s.substring(dashPos+1);
            coreToType.put(coreUnit, unitType);
            typeToCore.put(unitType, coreUnit);
        }
        CORE_TO_TYPE = ImmutableMap.copyOf(coreToType);
        TYPE_TO_CORE = ImmutableMultimap.copyOf(typeToCore);
    }

    public void TestUnitCategory() {
        Matcher prefixes = Pattern.compile(
            "yotta|zetta|exa|peta|tera|giga|mega|kilo|hecto|deka|deci|centi|milli|micro|nano|pico|femto|atto|zepto|yocto")
            .matcher("");
        final ImmutableSet<String> operators = ImmutableSet.of("square", "squared", "cubic", "cubed", "per");

        Matcher prefixesAndOperators = Pattern.compile(
            "-?(yotta|zetta|exa|peta|tera|giga|mega|kilo|hecto|deka|deci|centi|milli|micro|nano|pico|femto|atto|zepto|yocto)"
                + "|square-|-squared|cubic-|-cubed|per-").matcher("");
        Map<Status, Set<String>> statusToCodes = Validity.getInstance().getStatusToCodes(LstrType.unit);

        Map<String,String> unitToType = new TreeMap<>();
        TreeMultimap<String, String> typeToCore = TreeMultimap.create();
        Set<String> regular = statusToCodes.get(Status.regular);
        for (String s : regular) {
            int dashPos = s.indexOf('-');
            String unitType = s.substring(0,dashPos);
            String coreUnit = s.substring(dashPos+1);
            typeToCore.put(unitType, coreUnit);

            // break down units
            if (prefixesAndOperators.reset(coreUnit).find()) {
                unitToType.put(coreUnit, "composed");
//            } else if (coreUnit.contains("-")) {
//                unitToType.put(coreUnit, "simpleHyphen");
            } else {
                unitToType.put(coreUnit, "simple");
            }
        }
        System.out.println();
        Set<String> oddities = new TreeSet<>();
        for (Entry<String, String> entry : typeToCore.entries()) {
            final String coreUnit = entry.getValue();
            final String unitType = entry.getKey();
            final String type2 = unitToType.get(coreUnit);
            System.out.println(
                unitType 
                + "\t " + coreUnit
                + "\t" + type2);
            String[] parts = coreUnit.split("-");

            if ("composed".equals(type2)) {
                for (String part : parts) {
                    if (prefixes.reset(part).lookingAt()) {
                        continue;
                    }
                    if (operators.contains(part)) {
                        continue;
                    }
                    if ("simple".equals(unitToType.get(part))) {
                        continue;
                    }
                    oddities.add(part);
                }
            }
        }
        System.out.println("oddities: " + oddities);

        UnitConverter converter = SDI.getUnitConverter();
        Set<String> canConvert = new TreeSet<>(converter.canConvert());
        Set<String> cores = new TreeSet<>(typeToCore.values());

        cores.removeAll(canConvert);
        cores.removeAll(converter.baseUnits());
        assertEquals("Units we can't convert", Collections.EMPTY_SET, cores);
        canConvert.removeAll(typeToCore.values());
        assertEquals("Conversions for invalid units", Collections.EMPTY_SET, canConvert);
    }
}
