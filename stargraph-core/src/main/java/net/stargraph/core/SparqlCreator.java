package net.stargraph.core;

import net.stargraph.Utils;
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

        // find used variables
        List<String> usedVariables = new ArrayList<>();
        for (String v : varBindings.keySet()) {
            if (pattern.matches("^.*(?<!\\w)" + Pattern.quote(v) + "(?!\\w).*$") && varBindings.get(v).size() > 0) {
                usedVariables.add(v);
            }
        }

        if (usedVariables.size() <= 0) {
            return Arrays.asList(new ResolvedPattern(pattern, new HashMap<>()));
        }

        // create all possible permutations of bindings for sequence of usedVariables:
        List<List<String>> varPermutations = null;
        for (String v : usedVariables) {
            if (varPermutations == null) {
                varPermutations = varBindings.get(v).stream().map(x -> Arrays.asList(x)).collect(Collectors.toList());
            } else {
                varPermutations = Utils.cartesianProduct(varPermutations, varBindings.get(v));
            }
        }

        // replace pattern by these permutations
        List<ResolvedPattern> res = new ArrayList<>();
        for (List<String> varPermutation : varPermutations) {
            Map<String, String> usedBindings = new HashMap<>();
            String str = pattern;
            for (int i = 0; i < usedVariables.size(); i++) {
                String var = usedVariables.get(i);
                String repl = varPermutation.get(i);
                str = str.replaceAll("(?<!\\w)" + Pattern.quote(var) + "(?!\\w)", repl);
                usedBindings.put(var, repl);
            }
            res.add(new ResolvedPattern(str, usedBindings));
        }

        return res;
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

    public List<String> resolvePatternToStr(String pattern, Map<String, List<String>> varBindings) {
        return resolvePatternToStr(pattern, varBindings, Arrays.asList());
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

    public PathPattern createPathPattern(String startVar, List<String> propertyBindings, List<Boolean> inverseProperties, String endVar, String waypointVarPrefix) {
        if (propertyBindings.size() != inverseProperties.size()) {
            throw new AssertionError("propertyBindings and inverseProperties should have same size");
        }

        StringBuilder pattern = new StringBuilder();
        List<String> waypointVars = new ArrayList<>();

        pattern.append(startVar);

        for (int i = 0; i < propertyBindings.size(); i++) {
            String pStr = ((inverseProperties.get(i))? "^": "") + propertyBindings.get(i);

            if (i > 0) {
                String waypointVar = getNewVar(waypointVarPrefix);
                waypointVars.add(waypointVar);
                pattern.append(" " + waypointVar + " . " + waypointVar);
            }

            pattern.append(" " + pStr);
        }
        pattern.append(" " + endVar + " .");

        return new PathPattern(pattern.toString(), new ArrayList<>(), waypointVars);
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
}
