package net.stargraph.core;

import net.stargraph.query.Language;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SparqlCreator {
    public static class ResolvedPattern {
        private String str;
        private Map<String, String> usedBindings;

        public ResolvedPattern(String str, Map<String, String> usedBindings) {
            this.str = str;
            this.usedBindings = usedBindings;
        }

        public String getStr() {
            return str;
        }

        public Map<String, String> getUsedBindings() {
            return usedBindings;
        }
    }

    public static class PathPattern {
        private String pattern;
        private List<String> propertyVars;
        private List<String> waypointVars;

        public PathPattern(String pattern, List<String> propertyVars, List<String> waypointVars) {
            this.pattern = pattern;
            this.propertyVars = propertyVars;
            this.waypointVars = waypointVars;
        }

        public String getPattern() {
            return pattern;
        }

        public List<String> getPropertyVars() {
            return propertyVars;
        }

        public List<String> getWaypointVars() {
            return waypointVars;
        }
    }

    private Map<String, Integer> newVarCounter;

    public SparqlCreator() {
        this.newVarCounter = new HashMap<>();
    }

    public void resetNewVarCounter() {
        this.newVarCounter.clear();
    }

    public String getNewVar(String prefix) {
        int newValue = newVarCounter.computeIfAbsent(prefix, (p) -> 0) + 1;
        newVarCounter.put(prefix, newValue);
        return prefix + newValue;
    }


    public List<ResolvedPattern> resolvePattern(String pattern, Map<String, List<String>> varBindings) {
        List<String> variables = new ArrayList<>();
        List<String> bindings = null;
        for (String v : varBindings.keySet()) {
            if (pattern.matches("^.*(?<!\\w)" + Pattern.quote(v) + "(?!\\w).*$") && varBindings.get(v).size() > 0) {
                variables.add(v);
                if (bindings == null) {
                    bindings = varBindings.get(v);
                } else {
                    bindings = cartesianProduct(bindings, varBindings.get(v));
                }
            }
        }

        List<ResolvedPattern> res = new ArrayList<>();
        for (String binding : bindings) {
            Map<String, String> usedBindings = new HashMap<>();

            String[] bs = binding.split(" ");
            String str = pattern;
            for (int i = 0; i < variables.size(); i++) {
                String var = variables.get(i);
                String b = bs[i];

                str = str.replaceAll("(?<!\\w)" + Pattern.quote(var) + "(?!\\w)", b);
                usedBindings.put(var, b);
            }
            res.add(new ResolvedPattern(str, usedBindings));
        }

        return res;
    }

    public List<String> resolvePatternToStr(String pattern, Map<String, List<String>> varBindings) {
        return resolvePattern(pattern, varBindings).stream().map(rp -> rp.getStr()).collect(Collectors.toList());
    }

    public List<String> resolvePatternToStr(String pattern, Map<String, List<String>> varBindings, List<String> bindVars) {
        List<String> res = new ArrayList<>();
        for (ResolvedPattern rp : resolvePattern(pattern, varBindings)) {
            Map<String, List<String>> usedBindings = new HashMap<>();
            for (String v : rp.getUsedBindings().keySet()) {
                usedBindings.put(v, Arrays.asList(rp.getUsedBindings().get(v)));
            }

            res.add(rp.getStr() + " " + createBindStr(usedBindings, bindVars));
        }
        return res;
    }


    public String createBindStr(Map<String, List<String>> varBindings, List<String> bindVars) {

        List<String> bindStatements = new ArrayList<>();
        for (String bindVar : bindVars) {
            if (varBindings.containsKey(bindVar)) {
                StringJoiner innerJoiner = new StringJoiner(" || ", "BIND(", ")");
                for (String bound : varBindings.get(bindVar)) {
                    innerJoiner.add(bound + " AS " + bindVar);
                }
                bindStatements.add(innerJoiner.toString());
            }
        }
        return stmtJoin(bindStatements);
    }

    public String createRegexFilterStr(Map<String, List<String>> varFilters, boolean caseSensitive, boolean negate) {

        List<String> filterStatements = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : varFilters.entrySet()) {
            StringJoiner innerJoiner = new StringJoiner((negate)? " && ": " || ", "FILTER(", ")");
            for (String regexPattern : entry.getValue()) {
                innerJoiner.add(((negate)? "!": "") + "regex(" + entry.getKey() + ", \"" + regexPattern + "\""+ ((caseSensitive)? "" : ", \"i\"") + ")");
            }
            filterStatements.add(innerJoiner.toString());
        }
        return stmtJoin(filterStatements);
    }

    public String createEqualsFilterStr(Map<String, List<String>> varFilters, boolean negate) {

        List<String> filterStatements = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : varFilters.entrySet()) {
            StringJoiner innerJoiner = new StringJoiner((negate)? " && ": " || ", "FILTER(", ")");
            for (String value : entry.getValue()) {
                innerJoiner.add(entry.getKey() + ((negate)? " != ": " == ") + value);
            }
            filterStatements.add(innerJoiner.toString());
        }
        return stmtJoin(filterStatements);
    }


    public PathPattern createPathPattern(String startVar, int properties, String endVar, String propertyVarPrefix, String waypointVarPrefix) {
        StringBuilder pattern = new StringBuilder();
        List<String> propertyVars = new ArrayList<>();
        List<String> waypointVars = new ArrayList<>();

        pattern.append(startVar);

        for (int i = 0; i < properties; i++) {
            if (i > 0) {
                String waypointVar = getNewVar(waypointVarPrefix);
                waypointVars.add(waypointVar);
                pattern.append(" " + waypointVar + " . " + waypointVar);
            }
            String propertyVar = getNewVar(propertyVarPrefix);
            propertyVars.add(propertyVar);
            pattern.append(" " + propertyVar);
        }
        pattern.append(" " + endVar + " .");

        return new PathPattern(pattern.toString(), propertyVars, waypointVars);
    }

    public String createLangFilter(String var, List<Language> languages, boolean includeNotSpecified) {
        List<String> langTags = new ArrayList<>();
        languages.forEach(l -> langTags.add(l.code.toLowerCase()));
        if (includeNotSpecified) {
            langTags.add("");
        }

        return "FILTER( " + langTags.stream().map(lang -> "lang(" + var + ") = \"" + lang + "\"").collect(Collectors.joining(" || ")) + " )";
    }

    public String createLimit(int limit) {
        return (limit >= 0)? "LIMIT " + limit: "";
    }


    public String stmtJoin(List<String> statements) {
        StringJoiner stmtJoiner = new StringJoiner(" . ");
        for (String statement : statements) {
            stmtJoiner.add(statement);
        }
        return stmtJoiner.toString();
    }

    public String unionJoin(List<String> statements, boolean lineBreak) {
        StringJoiner stmtJoiner;
        if (lineBreak) {
            stmtJoiner = new StringJoiner("} UNION\n{", "{", "}");
        } else {
            stmtJoiner = new StringJoiner("} UNION {", "{", "}");
        }

        for (String statement : statements) {
            stmtJoiner.add(statement);
        }
        return stmtJoiner.toString();
    }

    private static List<String> cartesianProduct(List<String> x, List<String> y) {
        List<String> xy = new ArrayList<>();
        x.forEach(s1 -> y.forEach(s2 -> xy.add(s1.trim() + " " + s2.trim())));
        return xy;
    }
}
