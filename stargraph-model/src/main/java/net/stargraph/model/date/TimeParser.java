package net.stargraph.model.date;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

;

public class TimeParser {
    private static String replaceGroupNames(String text, String newPrefix) {
        return text.replaceAll("\\?<(\\w+?)>", "?<" + newPrefix + "$1>");
    }

    private static final String PREF = "(?<!\\w)";
    private static final String SUFF = "(?!\\w)";

    private static String DAY_PATTERN = PREF + "(?<dd>\\d\\d)\\.(?<mm>\\d\\d)\\.(?<yyyy>[1-2]\\d\\d\\d)" + SUFF;
    private static String YEAR_PATTERN = PREF + "(?<year>[1-2]\\d\\d\\d)" + SUFF;
    private static String BC_PATTERN = PREF + "(?<bc>\\d+)\\s+bc" + SUFF;
    private static String AD_PATTERN1 = PREF + "(?<ad1>\\d+)\\s+ad" + SUFF;
    private static String AD_PATTERN2 = PREF + "ad\\s+(?<ad2>\\d+)" + SUFF;
    private static String CENTURY_PATTERN1 = PREF + "(?<century1>1)\\s*st\\s+century" + SUFF;
    private static String CENTURY_PATTERN2 = PREF + "(?<century2>2)\\s*nd\\s+century" + SUFF;
    private static String CENTURY_PATTERN3 = PREF + "(?<century3>3)\\s*rd\\s+century" + SUFF;
    private static String CENTURY_PATTERN4 = PREF + "(?<century4>\\d+)\\s*th\\s+century" + SUFF;

    private static String SIMPLE_DATE_PATTERN = "(?:"
            + Arrays.asList(
                YEAR_PATTERN,
                DAY_PATTERN,
                BC_PATTERN,
                AD_PATTERN1,
                AD_PATTERN2,
                CENTURY_PATTERN1,
                CENTURY_PATTERN2,
                CENTURY_PATTERN3,
                CENTURY_PATTERN4
            ).stream().collect(Collectors.joining("|"))
            + ")";

    private static String RANGE_PATTERN1 = "(?<range1>" + PREF + "between\\s+(?:the\\s+)?" + replaceGroupNames(SIMPLE_DATE_PATTERN, "rangestart1") + "\\s+and\\s+(?:the\\s+)?" + replaceGroupNames(SIMPLE_DATE_PATTERN, "rangeend1") + SUFF + ")";
    private static String RANGE_PATTERN2 = "(?<range2>" + PREF + "from\\s+(?:the\\s+)?" + replaceGroupNames(SIMPLE_DATE_PATTERN, "rangestart2") + "\\s+to\\s+(?:the\\s+)?" + replaceGroupNames(SIMPLE_DATE_PATTERN, "rangeend2") + SUFF + ")";
    private static String BEFORE_PATTERN = "(?<before>" + PREF + "before\\s+(?:the\\s+)?" + replaceGroupNames(SIMPLE_DATE_PATTERN, "before") + SUFF + ")";
    private static String AFTER_PATTERN = "(?<after>" + PREF + "after\\s+(?:the\\s+)?" + replaceGroupNames(SIMPLE_DATE_PATTERN, "after") + SUFF + ")";
    private static String SIMPLE_PATTERN = "(?<simple>" + replaceGroupNames(SIMPLE_DATE_PATTERN, "simple") + ")";

    private static String PATTERN = "(?:"
            + Arrays.asList(
                RANGE_PATTERN1,
                RANGE_PATTERN2,
                BEFORE_PATTERN,
                AFTER_PATTERN,
                SIMPLE_PATTERN
            ).stream().collect(Collectors.joining("|"))
            + ")";


    private static TimeRange parseYear(Calendar cal, String text) {
        int x = Integer.parseInt(text);
        cal.set(x, Calendar.JANUARY, 1);
        Date from = cal.getTime();
        cal.set(x, Calendar.DECEMBER, 31);
        Date to = cal.getTime();
        return TimeRange.fromTo(from, to);
    }

    private static TimeRange parseDay(Calendar cal, String day, String month, String year) {
        int d = Integer.parseInt(day);
        int m = Integer.parseInt(month);
        int y = Integer.parseInt(year);

        cal.set(y, m-1, d);
        Date from = cal.getTime();
        Date to = cal.getTime();
        return TimeRange.fromTo(from, to);
    }

    private static TimeRange parseCentury(Calendar cal, String text) {
        int x = Integer.parseInt(text);
        cal.set((100*(x-1))+1, Calendar.JANUARY, 1);
        Date from = cal.getTime();
        cal.set(100*x, Calendar.DECEMBER, 31);
        Date to = cal.getTime();
        return TimeRange.fromTo(from, to);
    }

    private static TimeRange parseSimple(Calendar cal, Matcher matcher, String prefix) {
        try {
            if (matcher.group(prefix + "year") != null) {
                return parseYear(cal, matcher.group(prefix + "year"));
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "dd") != null || matcher.group(prefix + "mm") != null || matcher.group(prefix + "yyyy") != null) {
                return parseDay(cal, matcher.group(prefix + "dd"), matcher.group(prefix + "mm"), matcher.group(prefix + "yyyy"));
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "bc") != null) {
                //TODO implement
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "ad1") != null) {
                //TODO implement
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "ad2") != null) {
                //TODO implement
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "century1") != null) {
                return parseCentury(cal, matcher.group(prefix + "century1"));
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "century2") != null) {
                return parseCentury(cal, matcher.group(prefix + "century2"));
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "century3") != null) {
                return parseCentury(cal, matcher.group(prefix + "century3"));
            }
        } catch (Exception e) {}
        try {
            if (matcher.group(prefix + "century4") != null) {
                return parseCentury(cal, matcher.group(prefix + "century4"));
            }
        } catch (Exception e) {}

        return null;
    }

    public List<TimeRange> parse(String text) {
        Calendar cal = Calendar.getInstance();
        List<TimeRange> res = new ArrayList<>();

        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(text.toLowerCase());

        while (matcher.find()) {
            if (matcher.group("range1") != null) {
                TimeRange start = parseSimple(cal, matcher, "rangestart1");
                TimeRange end = parseSimple(cal, matcher, "rangeend1");
                if (start != null || end != null) {
                    res.add(TimeRange.fromTo(start.getFrom(), end.getTo()));
                }
            }
            if (matcher.group("range2") != null) {
                TimeRange start = parseSimple(cal, matcher, "rangestart2");
                TimeRange end = parseSimple(cal, matcher, "rangeend2");
                if (start != null || end != null) {
                    res.add(TimeRange.fromTo(start.getFrom(), end.getTo()));
                }
            }
            if (matcher.group("before") != null) {
                TimeRange timeRange = parseSimple(cal, matcher, "before");
                if (timeRange != null) {
                    res.add(TimeRange.before(timeRange.getFrom()));
                }
            }
            if (matcher.group("after") != null) {
                TimeRange timeRange = parseSimple(cal, matcher, "after");
                if (timeRange != null) {
                    res.add(TimeRange.after(timeRange.getTo()));
                }
            }
            if (matcher.group("simple") != null) {
                TimeRange timeRange = parseSimple(cal, matcher, "simple");
                if (timeRange != null) {
                    res.add(timeRange);
                }
            }
        }

        return res;
    }
}