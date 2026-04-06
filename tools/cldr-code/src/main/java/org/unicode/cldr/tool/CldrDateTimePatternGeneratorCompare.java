package org.unicode.cldr.tool;

import com.ibm.icu.text.DateTimePatternGenerator;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrDateTimePatternGenerator;
import org.unicode.cldr.util.Factory;

/**
 * Compares the behavior of CldrDateTimePatternGenerator against ICU4J's DateTimePatternGenerator.
 */
@CLDRTool(
        alias = "compare-cldr-dtpg",
        description =
                "Compares CldrDateTimePatternGenerator against ICU4J DateTimePatternGenerator",
        url = CLDRURLS.TOOLSURL)
public class CldrDateTimePatternGeneratorCompare {
    private static final String[] SKELETONS = {
        "y",
        "yM",
        "yMd",
        "yMEd",
        "yMMM",
        "yMMMd",
        "yMMMEd",
        "yMMMM",
        "yMMMMd",
        "yMMMMEEEEd",
        "M",
        "Md",
        "MEd",
        "MMM",
        "MMMd",
        "MMMEd",
        "MMMM",
        "MMMMd",
        "MMMMEEEEd",
        "j",
        "jm",
        "jms",
        "Hm",
        "Hms",
        "hm",
        "hms",
        "Mdjm",
        "MMMdjm",
        "MMMMdjm",
        "yMdjms",
        "yMMMdjms",
        "yMMMMdjms",

        // New additions:
        // Era + Year combinations
        "Gy",
        "GyM",
        "GyMMM",
        "GyMMMd",

        // Different year types
        "u",
        "U",
        "r",

        // Quarters
        "Q",
        "yQ",
        "yQQQ",
        "yQQQQ",
        "q",
        "yq",
        "yqqq",
        "yqqqq",

        // Stand-alone Month and Weekday
        "L",
        "LLLL",
        "cccc",
        "eeee",

        // Week of Year and Week of Month
        "Yw",
        "Yww",
        "YwE",
        "W",
        "MMMMW",

        // Day of Year and Day of Week in Month
        "D",
        "yD",
        "F",
        "MF",

        // Different hour cycles and input symbols
        "h",
        "hh",
        "H",
        "HH",
        "K",
        "KK",
        "k",
        "kk",
        "jj",
        "jjj",
        "jjjj",
        "jjjjj",
        "jjjjjj",
        "J",
        "JJ",
        "C",
        "CC",
        "CCC",
        "CCCC",
        "CCCCC",
        "CCCCCC",

        // Minutes and Seconds
        "m",
        "s",
        "ss",
        "S",
        "SSS",
        "msS",
        "A",

        // Zones
        "z",
        "zzzz",
        "Z",
        "ZZZZ",
        "ZZZZZ",
        "O",
        "OOOO",
        "v",
        "vvvv",
        "V",
        "VV",
        "VVV",
        "VVVV",
        "X",
        "XXXX",
        "x",
        "xxxx",

        // Zone combinations
        "yMz",
        "yMv",
        "yMVVVV",
        "yMXXXX",

        // Unusual / non-availableFormats combinations
        "GE",
        "GQ",
        "MdH",
        "yMdHms",
        "yMdHmsZ",
        "EBhm",
        "hB",
        "hb",

        // Test field normalization/canonicalization
        "dM",
        "My",
        "g",
        "a",
    };

    private static final String[] CALENDARS = {
        "gregorian", "buddhist", "japanese", "roc", "islamic", "chinese"
    };

    public static void main(String[] args) throws IOException {
        String filter = args.length > 0 ? args[0] : ".*";

        CLDRConfig config = CLDRConfig.getInstance();
        Factory factory = config.getCldrFactory();
        Set<String> locales = factory.getAvailableLanguages();

        String outputDir = CLDRPaths.CHART_DIRECTORY + "/verify/dates/";
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filename = "dtpg_comparison.csv";

        System.out.println(
                "Generating comparison to " + outputDir + filename + " with filter " + filter);

        try (PrintWriter out = FileUtilities.openUTF8Writer(outputDir, filename)) {
            // Header
            out.println(
                    "Locale,Calendar,Skeleton,CldrDTPG Pattern,ICU4J DTPG Pattern,Match?,Trace");

            int count = 0;
            List<String> sortedLocales = new ArrayList<>(locales);
            Collections.sort(sortedLocales);

            for (String localeID : sortedLocales) {
                if (localeID.equals("root")) continue;
                if (!localeID.matches(filter)) continue;

                System.out.println("Processing locale: " + localeID);
                CLDRFile cldrFile = factory.make(localeID, true);

                for (String calendar : CALENDARS) {
                    CldrDateTimePatternGenerator cldrGen =
                            new CldrDateTimePatternGenerator(cldrFile, calendar, false);

                    DateTimePatternGenerator icuGen = cldrGen.getIcu4jGenerator();

                    for (String skeleton : SKELETONS) {
                        List<String> trace = new ArrayList<>();
                        String cldrPattern = cldrGen.getBestPattern(skeleton, trace);
                        String icuPattern = normalizePattern(icuGen.getBestPattern(skeleton));

                        String matchResult = getDifferenceReason(cldrPattern, icuPattern);
                        String traceStr = String.join(" | ", trace);

                        out.print(escapeCsv(localeID));
                        out.print(",");
                        out.print(escapeCsv(calendar));
                        out.print(",");
                        out.print(escapeCsv(skeleton));
                        out.print(",");
                        out.print(escapeCsv(cldrPattern));
                        out.print(",");
                        out.print(escapeCsv(icuPattern));
                        out.print(",");
                        out.print(escapeCsv(matchResult));
                        out.print(",");
                        out.print(escapeCsv(traceStr));
                        out.println();

                        count++;
                    }
                }
                out.flush();
            }
            System.out.println("Done! Generated " + count + " comparisons.");
        }
    }

    private static String getDifferenceReason(String cldr, String icu) {
        if (cldr.equals(icu)) return "YES";

        List<String> cldrFields = getFields(cldr);
        List<String> icuFields = getFields(icu);

        // 1. Check for Era mismatch
        boolean cldrHasG = cldrFields.stream().anyMatch(f -> f.startsWith("G"));
        boolean icuHasG = icuFields.stream().anyMatch(f -> f.startsWith("G"));
        if (cldrHasG != icuHasG) {
            return icuHasG ? "NO: Era added" : "NO: Era removed";
        }

        // 2. Check for Field Set mismatch (excluding Era)
        Set<Character> cldrFieldSet = new TreeSet<>();
        for (String f : cldrFields) if (!f.startsWith("G")) cldrFieldSet.add(f.charAt(0));
        Set<Character> icuFieldSet = new TreeSet<>();
        for (String f : icuFields) if (!f.startsWith("G")) icuFieldSet.add(f.charAt(0));

        if (!cldrFieldSet.equals(icuFieldSet)) {
            return "NO: Field set mismatch";
        }

        // 3. Check for Field Order mismatch
        List<Character> cldrOrder = new ArrayList<>();
        for (String f : cldrFields) cldrOrder.add(f.charAt(0));
        List<Character> icuOrder = new ArrayList<>();
        for (String f : icuFields) icuOrder.add(f.charAt(0));

        if (!cldrOrder.equals(icuOrder)) {
            return "NO: Field order mismatch";
        }

        // 4. Check for Field Length mismatch
        if (cldrFields.size() == icuFields.size()) {
            for (int i = 0; i < cldrFields.size(); i++) {
                String cF = cldrFields.get(i);
                String iF = icuFields.get(i);
                if (!cF.equals(iF)) {
                    return "NO: Field length mismatch: " + cF + " -> " + iF;
                }
            }
        }

        // 5. If everything else is the same, it must be literals or separators
        return "NO: Separator/Literal mismatch";
    }

    private static List<String> getFields(String pattern) {
        List<String> fields = new ArrayList<>();
        boolean inQuote = false;
        for (int i = 0; i < pattern.length(); ) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                inQuote = !inQuote;
                i++;
                continue;
            }
            if (inQuote) {
                i++;
                continue;
            }
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                int start = i;
                while (i < pattern.length() && pattern.charAt(i) == ch) {
                    i++;
                }
                fields.add(pattern.substring(start, i));
            } else {
                i++;
            }
        }
        return fields;
    }

    private static String normalizePattern(String pattern) {
        if (pattern == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < pattern.length(); ) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                sb.append(ch);
                inQuote = !inQuote;
                i++;
            } else if (inQuote) {
                sb.append(ch);
                i++;
            } else if (ch == 'E') {
                int count = 0;
                while (i < pattern.length() && pattern.charAt(i) == 'E') {
                    count++;
                    i++;
                }
                if (count <= 3) {
                    sb.append("E");
                } else {
                    for (int j = 0; j < count; j++) sb.append('E');
                }
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
