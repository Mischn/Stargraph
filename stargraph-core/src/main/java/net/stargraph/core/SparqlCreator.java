package net.stargraph.core;

import net.stargraph.model.Entity;
import net.stargraph.model.PropertyEntity;
import net.stargraph.model.PropertyPath;
import net.stargraph.query.Language;

import java.util.*;
import java.util.stream.Collectors;

public class SparqlCreator {
    private Map<String, Integer> newVarCounter;

    public SparqlCreator() {
        this.newVarCounter = new HashMap<>();
    }

    public static String formatURI(String uri) {
        uri = uri.trim();
        if (!uri.startsWith("<")) {
            uri = "<" + uri;
        }
        if (!uri.endsWith(">")) {
            uri = uri + ">";
        }
        return uri;
    }

    public static String formatVar(String var) {
        var = var.trim();
        if (!var.startsWith("?")) {
            var = "?" + var;
        }
        return var;
    }


    public void resetNewVarCounter() {
        this.newVarCounter.clear();
    }

    public String getNewVar(String prefix) {
        int newValue = newVarCounter.computeIfAbsent(prefix, (p) -> 0) + 1;
        newVarCounter.put(prefix, newValue);
        return formatVar(prefix + newValue);
    }




    public String createStmt(String subj, String pred, String obj) {
        return subj + " " + pred + " " + obj;
    }

    public String varBindingStmt(String var, List<String> boundURIs) {
        StringJoiner joiner = new StringJoiner(" ", "{", "}");
        for (String uri : boundURIs) {
            joiner.add(formatURI(uri));
        }
        return "values " + formatVar(var) + " " + joiner.toString();
    }

    public String createPred(List<Entity> properties) {
        StringJoiner joiner = new StringJoiner(") | (", "(", ")");
        for (Entity property : properties) {

            // PropertyEntity
            if (property instanceof PropertyEntity) {
                joiner.add(formatURI(property.getId()));
            } else
            // PropertyPath
            if (property instanceof PropertyPath) {
                PropertyPath propertyPath = (PropertyPath)property;
                StringJoiner joiner2 = new StringJoiner(" / ");
                for (int i = 0; i < propertyPath.getProperties().size(); i++) {
                    boolean inverse = propertyPath.getDirections().get(i).equals(PropertyPath.Direction.INCOMING);
                    joiner2.add(((inverse)? "^" : "") + formatURI(propertyPath.getProperties().get(i).getId()));
                }
                joiner.add(joiner2.toString());
            } else {
                throw new AssertionError("Unknown property");
            }
        }

        return joiner.toString();
    }

    public String createExactPred(List<String> alternativeURIs, int exactRange) {
        StringJoiner joiner = new StringJoiner(") / (", "(", ")");
        for (int i = 0; i < exactRange; i++) {
            StringJoiner joiner2 = new StringJoiner(" | ");
            for (String uri : alternativeURIs) {
                joiner2.add(formatURI(uri));
            }
            joiner.add(joiner2.toString());
        }
        return joiner.toString();
    }

    public String createPred(List<String> alternativeURIs, int range) {
        StringJoiner joiner = new StringJoiner(") | (", "(", ")");
        for (int i = 0; i < range; i++) {
            joiner.add(createExactPred(alternativeURIs, i+1));
        }
        return joiner.toString();
    }

    public String createExactPred(String predicatePrefix, String waypointPrefix, int exactRange) {
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < exactRange; i++) {
            if (i > 0) {
                String waypointVar = getNewVar(waypointPrefix);
                pattern.append(" " + waypointVar + " . " + waypointVar);
            }
            String predicateVar = getNewVar(predicatePrefix);
            pattern.append(" " + predicateVar);
        }

        return pattern.toString();
    }

    public List<String> createPreds(String predicatePrefix, String waypointPrefix, int range) {
        List<String> preds = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            preds.add(createExactPred(predicatePrefix, waypointPrefix,i+1));
        }
        return preds;
    }

    public String createBindStmt(String var, List<String> uris) {
        StringJoiner innerJoiner = new StringJoiner(" || ", "BIND(", ")");
        for (String uri : uris) {
            innerJoiner.add(formatURI(uri) + " AS " + formatVar(var));
        }
        return innerJoiner.toString();
    }


    public String createLangFilterStmt(String var, List<Language> languages, boolean includeNotSpecified) {
        List<String> langTags = new ArrayList<>();
        languages.forEach(l -> langTags.add(l.code.toLowerCase()));
        if (includeNotSpecified) {
            langTags.add("");
        }

        return "FILTER( " + langTags.stream().map(lang -> "lang(" + var + ") = \"" + lang + "\"").collect(Collectors.joining(" || ")) + " )";
    }

    public String createEqualsFilterStmt(String var, List<String> uris, boolean negate) {
        StringJoiner innerJoiner = new StringJoiner((negate)? " && ": " || ", "FILTER(", ")");
        for (String uri : uris) {
            innerJoiner.add(formatVar(var) + ((negate)? " != ": " == ") + formatURI(uri));
        }
        return innerJoiner.toString();
    }

    public String createLimit(int limit) {
        return (limit >= 0)? "LIMIT " + limit: "";
    }







    public String stmtJoin(List<String> statements, boolean lineBreak) {
        StringJoiner stmtJoiner;
        if (lineBreak) {
            stmtJoiner = new StringJoiner(" .\n");
        } else {
            stmtJoiner = new StringJoiner(" . ");
        }

        for (String statement : statements) {
            stmtJoiner.add(statement);
        }
        return stmtJoiner.toString();
    }

    public String stmtUnionJoin(List<String> statements, boolean lineBreak) {
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
