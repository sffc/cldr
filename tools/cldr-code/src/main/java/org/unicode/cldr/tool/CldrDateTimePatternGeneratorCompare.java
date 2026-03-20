package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.util.ULocale;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrDateTimePatternGenerator;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.CLDRPaths;

/**
 * Compares the behavior of CldrDateTimePatternGenerator against ICU4J's DateTimePatternGenerator.
 */
@CLDRTool(
    alias = "compare-cldr-dtpg",
    description = "Compares CldrDateTimePatternGenerator against ICU4J DateTimePatternGenerator",
    url = CLDRURLS.TOOLSURL)
public class CldrDateTimePatternGeneratorCompare {
    private static final String[] SKELETONS = {
        "y", "yM", "yMd", "yMEd", "yMMM", "yMMMd", "yMMMEd", "yMMMM", "yMMMMd", "yMMMMEEEEd",
        "M", "Md", "MEd", "MMM", "MMMd", "MMMEd", "MMMM", "MMMMd", "MMMMEEEEd",
        "j", "jm", "jms", "Hm", "Hms", "hm", "hms",
        "Mdjm", "MMMdjm", "MMMMdjm", "yMdjms", "yMMMdjms", "yMMMMdjms"
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
        
        System.out.println("Generating comparison to " + outputDir + filename + " with filter " + filter);
        
        try (PrintWriter out = FileUtilities.openUTF8Writer(outputDir, filename)) {
            // Header
            out.println("Locale,Calendar,Skeleton,CldrDTPG Pattern,ICU4J DTPG Pattern,Match?");

            int count = 0;
            List<String> sortedLocales = new ArrayList<>(locales);
            Collections.sort(sortedLocales);

            for (String localeID : sortedLocales) {
                if (localeID.equals("root")) continue;
                if (!localeID.matches(filter)) continue;
                
                System.out.println("Processing locale: " + localeID);
                CLDRFile cldrFile = factory.make(localeID, true);

                for (String calendar : CALENDARS) {
                    CldrDateTimePatternGenerator cldrGen = new CldrDateTimePatternGenerator(cldrFile, calendar, true);
                    
                    DateTimePatternGenerator icuGen = cldrGen.getIcu4jGenerator();

                    for (String skeleton : SKELETONS) {
                        String cldrPattern = cldrGen.getBestPattern(skeleton);
                        String icuPattern = icuGen.getBestPattern(skeleton);
                        
                        boolean match = cldrPattern.equals(icuPattern);
                        
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
                        out.print(match ? "YES" : "NO");
                        out.println();
                        
                        count++;
                    }
                }
                out.flush();
            }
            System.out.println("Done! Generated " + count + " comparisons.");
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
